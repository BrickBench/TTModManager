package com.opengg.modmanager;

import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.swing.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.List;

public class ModListFileManager {
    public static void readModList(){
        var file = new File(Util.getFromMainDirectory("mods.json"));
        try {
            if(!file.exists()) file.createNewFile();

            System.out.println("Reading mod file");

            String jsonTxt = IOUtils.toString(new FileInputStream(file), Charset.defaultCharset());

            if(jsonTxt.isEmpty()) return;

            var json = new JSONObject(jsonTxt);

            var mods = json.getJSONArray("mods");
            var loadedSources = new LinkedHashMap<String, List<Mod>>();
            for(var jmod : mods){
                var jobj = (JSONObject) jmod;
                var name = jobj.getString("id");
                var source = jobj.getString("source");
                var enabled = jobj.getBoolean("enabled");


                System.out.println("Searching for mod " + name);

                List<Mod> sourceList;
                if(loadedSources.containsKey(source)){
                    sourceList = loadedSources.get(source);
                    System.out.println("Found in existing source " + source);
                }else{
                    try {
                        sourceList = ModUtil.loadMod(new File(source), false);
                        loadedSources.put(source, sourceList);
                    }catch (IOException e){

                        continue;
                    }
                }

                Mod realMod = null;
                for(var mod : sourceList){
                    if(mod.id().equals(name)){
                        mod.setLoaded(enabled);
                        realMod = mod;
                        break;
                    }
                }

                if(realMod == null) JOptionPane.showMessageDialog(null, "Could not find mod " + name + " in source file " + source);
            }

            System.out.println("Finished loading mod file");

            loadedSources.forEach((s, l) -> l.forEach(TTModManager.CURRENT::registerMod));
        } catch (IOException e) {
            System.out.println("Failed to load mods.json file: " + e.getMessage());
        }
    }

    public static void writeModList(List<Mod> mods){
        var file = new File(Util.getFromMainDirectory("mods.json"));

        var root = new JSONObject();
        var array = new JSONArray();

        for(var mod : mods){
            var obj = new JSONObject();
            obj.put("id", mod.id());
            obj.put("source", mod.sourceFile());
            obj.put("enabled", mod.isLoaded());

            array.put(obj);
        }

        root.put("mods", array);

        try(var writer = new BufferedWriter(new FileWriter(file))) {
            writer.write(root.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
