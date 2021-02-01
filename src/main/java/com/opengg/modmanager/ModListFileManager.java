package com.opengg.modmanager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class ModListFileManager {
    public static List<Mod> readModList(){
        var file = new File("mods.txt");
        try {
            var lines = Files.readAllLines(file.toPath());
            var list = new ArrayList<Mod>();
            for(var line : lines){
                if(line.isEmpty()) continue;

                var blocks = line.split("\\|");
                list.add(new Mod(blocks[0], Mod.ModType.valueOf(blocks[2]), Boolean.parseBoolean(blocks[1])));
            }

            return list;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return List.of();
    }

    public static void writeModList(List<Mod> mods){
        var file = new File("mods.txt");

        try(var os = new FileWriter(file)) {

            for(var mod : mods){
                os.write(mod.getPath());
                os.write("|");
                os.write(Boolean.toString(mod.isLoaded()));
                os.write("|");
                os.write(mod.getType().toString());
                os.write("\n");
            }
            os.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
