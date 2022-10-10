package modmanager;

import modmanager.ui.BottomPane;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;

public class ModListFileManager {
    public static void readModList(){
        var file = Util.getFromMainDirectory("mods.json");
        try {
            if (!Files.exists(file)) Files.createFile(file);

            BottomPane.log("Reading mod file");
            String jsonTxt = IOUtils.toString(new FileInputStream(file.toFile()), Charset.defaultCharset());

            if(jsonTxt.isEmpty()) return;

            var json = new JSONObject(jsonTxt);

            var mods = json.getJSONArray("mods");
            var loadedSources = new LinkedHashMap<Path, List<Mod>>();

            for(var jmod : mods){
                var jobj = (JSONObject) jmod;
                var name = jobj.getString("id");
                var source = Path.of(jobj.getString("source"));
                var enabled = jobj.getBoolean("enabled");

                BottomPane.log("Searching for mod " + name);

                List<Mod> sourceList;
                if(loadedSources.containsKey(source)){
                    sourceList = loadedSources.get(source);
                    BottomPane.log("Found in existing source " + source);
                }else{
                    try {
                        sourceList = ModUtil.loadSource(source, false);
                        loadedSources.put(source, sourceList);
                    }catch (IOException e){
                        continue;
                    }
                }

                for(var mod : sourceList){
                    if(mod.id().equals(name)){
                        mod.setEnabled(enabled);
                        ModManager.registerMod(mod);
                        break;
                    }
                }
            }

            BottomPane.log("Finished loading mod file");
            TTModManager.CURRENT.bottomPane.setProgressString("Finished loading mod file");
           // ModSorter.sortMods(ModManager.getLoadedMods());

        } catch (IOException e) {
            BottomPane.log("Failed to load mods.json file: " + e.getMessage());
            TTModManager.CURRENT.bottomPane.setProgressString("Failed to read mod file");
        }
    }

    public static void writeModList(List<Mod> mods){
        var file = Util.getFromMainDirectory("mods.json");

        var root = new JSONObject();
        var array = new JSONArray();

        for(var mod : mods){
            var obj = new JSONObject();
            obj.put("id", mod.id());
            obj.put("source", mod.sourceFile());
            obj.put("enabled", mod.isEnabled());

            array.put(obj);
        }

        root.put("mods", array);

        try(var writer = new BufferedWriter(new FileWriter(file.toFile()))) {
            writer.write(root.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
