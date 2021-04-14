package modmanager;

import clojure.core.IVecImpl;
import modmanager.ui.BottomPane;
import javafx.scene.control.Alert;
import org.sat4j.core.VecInt;
import org.sat4j.maxsat.WeightedMaxSatDecorator;
import org.sat4j.pb.SolverFactory;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.IVecInt;
import org.sat4j.specs.TimeoutException;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class ModManager {
    private static List<Mod> modList = new ArrayList<>();

    public static void addNewMod(File modFile){
        try{
            var mods = ModUtil.loadSource(modFile, true);
            for(var mod : mods){
                registerMod(mod);
            }
        }catch (Exception e){
            var alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Failed to load mods in modfile " + modFile + ": " + e.getMessage());
            alert.showAndWait();
        }
    }

    public static void registerMod(Mod mod){
        if(getLoadedMods().stream().anyMatch(m -> m.id().equals(mod.id()))) return;

        mod.enabled().addListener(c ->  TTModManager.CURRENT.modTable.setConflicts(rescanConflicts(getLoadedMods())));

        getLoadedMods().add(mod);

        refreshModList();
    }

    public static Optional<Mod> getByID(String id){
        return getLoadedMods().stream().filter(m -> m.id().equals(id)).findFirst();
    }

    public static void refreshModList(){
        TTModManager.CURRENT.modTable.setConflicts(rescanConflicts(getLoadedMods()));
        TTModManager.CURRENT.modTable.setModList(getLoadedMods());
        TTModManager.CURRENT.rightPane.refreshSourceList(getLoadedMods());
    }



    public static void sortMods(List<Mod> mods){
        //Current testing with Sat4
        var solver = new WeightedMaxSatDecorator(SolverFactory.newLight());
        solver.setTimeout(60);
        solver.newVar(mods.size() * mods.size() * 2);
        solver.setExpectedNumberOfClauses(mods.size() * mods.size() * 2);

        //2N^2 variables, each indicates that one mod is after the other in the mod order.
        //This first set of clauses attempts to maintain the current ordering of the list.
        //All of these are soft clauses, as the ordering is ideally mantained but is not required.
        //This current test is to ensure that the ordering is maintained when there are no dependencies
        int currentVar = 1;
        var debugConversions = new ArrayList<String>();
        for(int i = 0; i < mods.size(); i++){
            for(int j = i + 1; j < mods.size(); j++){
                try {

                    //Each new variable (currentVar, currentVar + 1) represents a relationship between two mods.
                    //The first one says that the first mod is in front of the second in the current list.
                    //The second one says that the first mod is after the second.

                    //First clause prioritizes the current ordering
                    solver.addSoftClause(new VecInt(new int[]{currentVar}));

                    //Second clause can have at most one true variable (one mod cannot be both in front and behind another).
                    solver.addSoftAtMost(new VecInt(new int[]{currentVar, currentVar + 1}), 1);

                    //For debugging.
                    debugConversions.add(mods.get(j).name() + " after " + mods.get(i).name());
                    debugConversions.add(mods.get(j).name() + " before " + mods.get(i).name());
                } catch (ContradictionException e) {
                    e.printStackTrace();
                }
                currentVar += 2;
            }
        }

        try {
            //When there are no dependencies, the problem should be completely satisfiable, since the
            //current ordering is 100% valid.
            if(solver.isSatisfiable()){
                for(var variable : solver.model()){
                    var clauseIdx = variable > 0 ? variable : -variable;
                    if(variable < 0){
                        System.out.println("NOT " + debugConversions.get(clauseIdx - 1));
                    }else{
                        System.out.println(debugConversions.get(clauseIdx - 1));
                    }
                }
            }
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

        //This is the old implementation.
        //Currently a two step process, does not support maintaining the current goal order and potentially bad performance.
        //This will be simplified with a partial-MaxSAT solver.
        var newModList = new ArrayList<Mod>();
        var original = new ArrayList<>(mods);

        while(!original.isEmpty()){
            var mod = original.get(0);
            original.remove(mod);
            if(mod.dependencies().isEmpty()){
                mod.modOrder().set(newModList.size());
                newModList.add(mod);
            }else{
                if(mod.dependencies().stream().allMatch(dep -> newModList.stream().map(Mod::id).anyMatch(nm -> nm.equals(dep)))){
                    mod.modOrder().set(newModList.size());
                    newModList.add(mod);
                }else{
                    if(original.isEmpty()){
                        BottomPane.log("Failed to find dependency for mod " + mod.id());
                        mod.setEnabled(false);
                        mod.modOrder().set(newModList.size());
                        newModList.add(mod);
                    }else{
                        original.add(1, mod);
                    }
                }
            }
        }

        modList = newModList;
        modList.sort(Comparator.comparingInt(Mod::getModOrder));
        refreshModList();
    }

    public static List<ConflictEntry> rescanConflicts(List<Mod> mods){
        mods = mods.stream().filter(Mod::isEnabled).collect(Collectors.toList());

        var conflicts = new ArrayList<ConflictEntry>();
        for(var mod : mods){
            for(var modBefore : mods.stream().limit(mods.indexOf(mod)).collect(Collectors.toList())){
                var newConflict = new ConflictEntry(modBefore, mod, new ArrayList<>(), ConflictEntry.Type.CONFLICTING_FILES);

                for(var previousFile : modBefore.editedFiles()){
                    if(mod.editedFiles().contains(previousFile)){
                        newConflict.conflictItems.add(previousFile);
                    }
                }

                if(!newConflict.conflictItems.isEmpty() && !mod.dependencies().contains(modBefore.id())){
                    conflicts.add(newConflict);
                }
            }
        }

        for(var mod : mods){
            var newConflict = new ConflictEntry(null, mod, new ArrayList<>(), ConflictEntry.Type.MISSING_DEPENDENCY);
            for(var dep : mod.dependencies()){
                var depMod = getByID(dep);
                if(depMod.isEmpty()){
                    newConflict.conflictItems.add(dep);
                }else if(!depMod.get().isEnabled()){
                    conflicts.add(new ConflictEntry(depMod.get(), mod, List.of(), ConflictEntry.Type.DEPENDENT_DISABLED));
                }
            }
        }

        return conflicts;
    }

    public static record ConflictEntry(Mod original, Mod conflicting, List<String> conflictItems, Type type){
        public enum Type{
            DEPENDENT_DISABLED, CONFLICTING_FILES, MISSING_DEPENDENCY
        }
    }

    public static void writeModList(){
        ModListFileManager.writeModList(getLoadedMods());
    }

    public static List<Mod> getLoadedMods(){
        return modList;
    }
}
