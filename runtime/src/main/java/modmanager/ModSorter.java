package modmanager;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import org.sat4j.core.VecInt;
import org.sat4j.maxsat.WeightedMaxSatDecorator;
import org.sat4j.pb.SolverFactory;

public class ModSorter {
    public static List<Mod> sortMods(List<Mod> mods) {
        record ModOrder(int first, int second) {};

        //Initializes the solver with n^2 variables
        var solver = new WeightedMaxSatDecorator(SolverFactory.newLight());
        solver.setTimeout(60);
        solver.newVar(mods.size() * mods.size());
        solver.setExpectedNumberOfClauses(mods.size() * mods.size() * mods.size());

        //Both maps map a pair of mods (ordered) to a variable in the SAT solver.
        int currentVar = 1;
        var modVariablesMap = new HashMap<ModOrder, Integer>();
        var modVariablesMapReverse = new HashMap<Integer, ModOrder>();

        //First, generate the soft constraints that define the current mod order.
        for (int i = 0; i < mods.size(); i++) {
            for (int j = i + 1; j < mods.size(); j++) {
                //For every pair of mods, it generates a soft clause that attempts to keep them ordered
                try {

                    //Generates a new variable to store the order of mods i,j
                    var newVar = currentVar++;
                    modVariablesMap.put(new ModOrder(i, j), newVar);
                    modVariablesMapReverse.put(newVar, new ModOrder(i, j));

                    var individualClause = modVariablesMap.get(new ModOrder(i, j));
                    solver.addSoftClause(new VecInt(new int[]{individualClause}));

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        //In the second step, the hard constraints resulting from the dependencies are added.
        for (var mod : mods) {
            //For every individual dependency, create a hard clause.
            for (var dep : mod.dependencies()) {
                var depAsMod = mods.stream().filter(m -> m.id().equals(dep)).findFirst().get();

                var ogIdx = mods.indexOf(mod);
                var depIdx = mods.indexOf(depAsMod);

                var variable = 0;

                //Adds a single-variable hard clause, either directly (if the mods are already in the
                //correct order) or as a negation if not.
                if (depIdx < ogIdx) {
                    variable = modVariablesMap.get(new ModOrder(depIdx, ogIdx));
                } else {
                    variable = -modVariablesMap.get(new ModOrder(ogIdx, depIdx));
                }

                try {
                    solver.addHardClause(new VecInt(new int[]{variable}));
                } catch (Exception e) {
                    //Direct circular dependency, returning
                    return mods;
                }
            }
        }


        //In the third step, the hard constraints from the natural rules of ordering (if a after b and b after c, then a after c) are set up
        for (int i = 0; i < mods.size(); i++) {
            for (int j = i + 1; j < mods.size(); j++) {
                for (int k = j + 1; k < mods.size(); k++) {
                    var ijVal = modVariablesMap.get(new ModOrder(i, j));
                    var jkVal = modVariablesMap.get(new ModOrder(j, k));
                    var ikVal = modVariablesMap.get(new ModOrder(i, k));

                    //Create new clause. This clause encodes the CNF version of (ij & jk) -> ik as -ij | -jk | ik
                    //(if I is before J and J before K, then I before K)
                    //Also adds the reverse: (-ij & -jk) -> -ik,  ij | jk | -ik
                    //(if I not before J and J not before K, then I not before K);
                    try{
                        solver.addHardClause(new VecInt(new int[]{-ijVal, -jkVal, ikVal}));
                        solver.addHardClause(new VecInt(new int[]{ijVal, jkVal, -ikVal}));
                    } catch (Exception e) {
                        //Indirect circular dependency, returning
                        return mods;
                    }
                }
            }
        }

        var newModList = new ArrayList<Mod>();
        var original = new ArrayList<>(mods);

        try{
            if(!solver.isSatisfiable()){
                //No valid ordering was found
                System.out.println("No path found");
                return mods;
            }else{
                //Using the ordering developed in the SAT solver, recreate the mod list.
                while (!original.isEmpty()) {
                    //For every mod remaining to be added, checks if it goes after any other mod that has not
                    //yet been added to the final list.
                    //This can also identify circular dependencies

                    boolean anyNewModsAdded = false;
                    for (int i = 0; i < original.size(); i++) {
                        var mod = original.get(i);
                        var srcIdx = mods.indexOf(mod);

                        var foundRemainingAddition = false;
                        for (var satVariable : solver.model()) {

                            //Gets the intended ordering for the mods this variable represents
                            var correspondingPair = modVariablesMapReverse.get(Math.abs(satVariable));
                            var firstMod = satVariable > 0 ? correspondingPair.first : correspondingPair.second;
                            var secondMod = satVariable > 0 ? correspondingPair.second : correspondingPair.first;

                            //If the current mod being checked is meant to go after a mod that has yet to be added, stop
                            if (secondMod == srcIdx && !newModList.contains(mods.get(firstMod))) {
                                foundRemainingAddition = true;
                            }
                        }

                        //No mod is meant to go before this one, so add it to the final list
                        //and remove it from the original list.
                        if (!foundRemainingAddition) {
                            original.remove(mod);
                            newModList.add(mod);

                            anyNewModsAdded = true;
                        }
                    }

                    //If a circular dependency is found, stop the process.
                    if (!anyNewModsAdded) {
                        System.out.println("Circular dependencies identified, bailing");
                        newModList.addAll(original);

                        break;
                    }
                }
            }
        }catch (Exception e) {
            e.printStackTrace();
        }

        //Sets the order property of the new list
        for (int i = 0; i < newModList.size(); i++) {
            newModList.get(i).modOrder().set(i);
        }

        return newModList;
    }


    //Creates a mod wit the given ID and dependencies.
    //This exists to not include unnecessary fields for testing sorting.
    public static Mod quickMod(String id, String... dependencies){
        return new Mod("","",id,"","",Path.of(""),Path.of(""),new SimpleBooleanProperty(true), Mod.Type.TT_MM,
                new SimpleIntegerProperty(0), List.of(), List.of(dependencies));
    }

    public static void printMods(List<Mod> mods){
        for(var mod : mods){
            System.out.println(mod.id() + ": " + mod.dependencies());
        }
    }

    public static boolean modListsEqual(List<Mod> check, List<Mod> expect){
        for(int i = 0; i < expect.size(); i++){
            if(!expect.get(i).id().equals(check.get(i).id())){
                System.out.println("FALSE! Expected");
                printMods(expect);
                System.out.println("Got");
                printMods(check);
                return false;
            }
        }

        return true;
    }

    public static void runSorterTests(){
        var directMatch = List.of(
                quickMod("ModA"),
                quickMod("ModB"),
                quickMod("ModC"),
                quickMod("ModD"),
                quickMod("ModE"),
                quickMod("ModF"));

        System.out.println("Does a list with no dependencies pass unedited: " + modListsEqual(ModSorter.sortMods(directMatch), directMatch));

        var directMatchDeps = List.of(
                quickMod("ModA"),
                quickMod("ModB"),
                quickMod("ModC", "ModB"),
                quickMod("ModD"),
                quickMod("ModE", "ModC"),
                quickMod("ModF"));


        System.out.println("Does a list with a single satisfied dependency pass unedited: " + modListsEqual(ModSorter.sortMods(directMatchDeps), directMatchDeps));

        var simpleSwap = List.of(
                quickMod("ModA", "ModB"),
                quickMod("ModB"));

        var simpleSwapGoal = List.of(
                quickMod("ModB"),
                quickMod("ModA", "ModB"));

        System.out.println("Does a simple reordering take place: " + modListsEqual(ModSorter.sortMods(simpleSwap), simpleSwapGoal));

        var longSwap = List.of(
                quickMod("ModA"),
                quickMod("ModB", "ModE"),
                quickMod("ModC"),
                quickMod("ModD"),
                quickMod("ModE"),
                quickMod("ModF"));

        var longSwapGoal = List.of(
                quickMod("ModA"),
                quickMod("ModC"),
                quickMod("ModD"),
                quickMod("ModE"),
                quickMod("ModB", "ModE"),
                quickMod("ModF"));

        System.out.println("Does a reordering over many steps take place: " + modListsEqual(ModSorter.sortMods(longSwap), longSwapGoal));

        var longMultiSwap = List.of(
                quickMod("ModA"),
                quickMod("ModB", "ModE", "ModF"),
                quickMod("ModC"),
                quickMod("ModD"),
                quickMod("ModE"),
                quickMod("ModF"));

        var longMultiSwapGoal = List.of(
                quickMod("ModA"),
                quickMod("ModC"),
                quickMod("ModD"),
                quickMod("ModE"),
                quickMod("ModF"),
                quickMod("ModB", "ModE", "ModF")
        );

        System.out.println("Does a reordering over many steps take place when multiple dependencies for one mod are used: " + modListsEqual(ModSorter.sortMods(longMultiSwap), longMultiSwapGoal));

        var modChainValid = List.of(
                quickMod("ModA"),
                quickMod("ModB", "ModA"),
                quickMod("ModC", "ModB"),
                quickMod("ModD"),
                quickMod("ModE"),
                quickMod("ModF"));


        System.out.println("Does a list with a chain of satisfied dependencies pass unedited: " + modListsEqual(ModSorter.sortMods(modChainValid), modChainValid));

        var modChain = List.of(
                quickMod("ModA", "ModB"),
                quickMod("ModB", "ModE"),
                quickMod("ModC"),
                quickMod("ModD"),
                quickMod("ModE"),
                quickMod("ModF"));

        var modChainGoal = List.of(
                quickMod("ModC"),
                quickMod("ModD"),
                quickMod("ModE"),
                quickMod("ModB", "ModE"),
                quickMod("ModA", "ModB"),
                quickMod("ModF"));

        System.out.println("Does a reordering take place when A depends on B and B on E: " + modListsEqual(ModSorter.sortMods(modChain), modChainGoal));

        var doubleDep = List.of(
                quickMod("ModA", "ModE"),
                quickMod("ModB", "ModE"),
                quickMod("ModC"),
                quickMod("ModD"),
                quickMod("ModE"),
                quickMod("ModF"));

        var doubleDepGoal = List.of(
                quickMod("ModC"),
                quickMod("ModD"),
                quickMod("ModE"),
                quickMod("ModA", "ModE"),
                quickMod("ModB", "ModE"),
                quickMod("ModF"));

        System.out.println("Does a reordering take place when two mods depend on the same other mod: " + modListsEqual(ModSorter.sortMods(doubleDep), doubleDepGoal));

        var doubleDepSatisfied = List.of(
                quickMod("ModA"),
                quickMod("ModB"),
                quickMod("ModC"),
                quickMod("ModD", "ModA"),
                quickMod("ModE", "ModA"),
                quickMod("ModF"));

        System.out.println("Does list with two mods depending on another mod that is atisfied pass unedited: " + modListsEqual(ModSorter.sortMods(doubleDepSatisfied), doubleDepSatisfied));

        var chainDoubleDep = List.of(
                quickMod("ModA", "ModB"),
                quickMod("ModB", "ModE"),
                quickMod("ModC", "ModE"),
                quickMod("ModD"),
                quickMod("ModE"),
                quickMod("ModF"));

        var chainDoubleDepGoal = List.of(
                quickMod("ModD"),
                quickMod("ModE"),
                quickMod("ModB", "ModE"),
                quickMod("ModA", "ModB"),
                quickMod("ModC", "ModE"),
                quickMod("ModF"));

        System.out.println("Does a reordering take place when two mods depend on the same other mod, and another depends on one of the two: " + modListsEqual(ModSorter.sortMods(chainDoubleDep), chainDoubleDepGoal));

        var circularDep = List.of(
                quickMod("ModA", "ModB"),
                quickMod("ModB", "ModA"),
                quickMod("ModC"),
                quickMod("ModD"),
                quickMod("ModE"),
                quickMod("ModF"));


        System.out.println("Does a list with a single step circular dependency stay unedited: " + modListsEqual(ModSorter.sortMods(circularDep), circularDep));

        var multiCircularDep = List.of(
                quickMod("ModA", "ModB"),
                quickMod("ModB", "ModC"),
                quickMod("ModC", "ModA"),
                quickMod("ModD"),
                quickMod("ModE"),
                quickMod("ModF"));


        System.out.println("Does a list with a multiple step circular dependency stay unedited: " + modListsEqual(ModSorter.sortMods(multiCircularDep), multiCircularDep));

        var gapMultiCircularDep = List.of(
                quickMod("ModA", "ModC"),
                quickMod("ModB"),
                quickMod("ModC", "ModE"),
                quickMod("ModD"),
                quickMod("ModE", "ModA"),
                quickMod("ModF"));


        System.out.println("Does a list with a multiple step circular dependency with a gap stay unedited: " + modListsEqual(ModSorter.sortMods(gapMultiCircularDep), gapMultiCircularDep));

        var test = List.of(
                quickMod("ModA", "ModJ"),
                quickMod("ModB"),
                quickMod("ModC", "ModI"),
                quickMod("ModD", "ModH"),
                quickMod("ModE", "ModH"),
                quickMod("ModF"),
                quickMod("ModG","ModH"),
                quickMod("ModH"),
                quickMod("ModI", "ModH"),
                quickMod("ModJ", "ModI"),
                quickMod("ModK", "ModB"),
                quickMod("ModL"),
                quickMod("ModM", "ModO"),
                quickMod("ModN", "ModO"),
                quickMod("ModO"),
                quickMod("ModP", "ModB")


                );

        printMods(sortMods(test));
    }
}
