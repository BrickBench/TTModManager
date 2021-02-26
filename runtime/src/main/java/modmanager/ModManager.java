package modmanager;

import modmanager.ui.BottomPane;
import javafx.scene.control.Alert;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
        if(modList.stream().anyMatch(m -> m.name().equals(mod.name()))) return;

        mod.enabled().addListener(c ->  TTModManager.CURRENT.modTable.setConflicts(rescanConflicts()));

        modList.add(mod);

        refreshModList();
    }

    public static Optional<Mod> getByID(String id){
        return modList.stream().filter(m -> m.id().equals(id)).findFirst();
    }

    public static void refreshModList(){
        TTModManager.CURRENT.modTable.setConflicts(rescanConflicts());
        TTModManager.CURRENT.modTable.setModList(modList);
        TTModManager.CURRENT.rightPane.refreshSourceList(modList);
    }

    public static void sortMods(){
        var newModList = new ArrayList<Mod>();

        while(!modList.isEmpty()){
            var mod = modList.get(0);
            modList.remove(mod);
            if(mod.dependencies().isEmpty()){
                mod.modOrder().set(newModList.size());
                newModList.add(mod);
            }else{
                if(mod.dependencies().stream().allMatch(dep -> newModList.stream().map(Mod::id).anyMatch(nm -> nm.equals(dep)))){
                    mod.modOrder().set(newModList.size());
                    newModList.add(mod);
                }else{
                    if(modList.isEmpty()){
                        BottomPane.log("Failed to find dependency for mod " + mod.id());
                        mod.setEnabled(false);
                        mod.modOrder().set(newModList.size());
                        newModList.add(mod);
                    }else{
                        modList.add(1, mod);
                    }
                }
            }
        }

        modList.addAll(newModList);

        modList.sort(Comparator.comparingInt(Mod::getModOrder));
        refreshModList();
    }

    public static List<ConflictEntry> rescanConflicts(){
        var mods = ModManager.getLoadedMods().stream().filter(Mod::isEnabled).collect(Collectors.toList());

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
        ModListFileManager.writeModList(modList);
    }

    public static List<Mod> getLoadedMods(){
        return modList;
    }
}
