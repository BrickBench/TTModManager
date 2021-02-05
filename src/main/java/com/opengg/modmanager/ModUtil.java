package com.opengg.modmanager;

import javafx.beans.property.SimpleBooleanProperty;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ModUtil {
    public static List<Mod> loadMod(File path, boolean askForPerms) throws IOException {
        System.out.println("Loading source " + path.getAbsolutePath());
        var newSpace = createModSpace(path);
        var mods = searchForMods(newSpace, path, askForPerms);
        return mods;
    }

    private static File createModSpace(File path) throws ZipException {
        if(path.isFile() && path.getAbsolutePath().toLowerCase().endsWith(".zip")){
            new ZipFile(path).extractAll(Util.getFromMainDirectory("Mods\\") + FilenameUtils.removeExtension(path.getName()));
            return new File(Util.getFromMainDirectory("Mods\\") + FilenameUtils.removeExtension(path.getName()));
        }else if(path.isDirectory()){
            return path;
        }
        return null;
    }

    private static List<Mod> searchForMods(File search, File source, boolean askForPerms) throws IOException {
        var allTTMMMods = Files.find(search.toPath(),
                Integer.MAX_VALUE,
                (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.getFileName().toString().equalsIgnoreCase("TTModDef.json"))
                .collect(Collectors.toList());

        var allReloadedMods = Files.find(search.toPath(),
                Integer.MAX_VALUE,
                (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.getFileName().toString().equalsIgnoreCase("ModConfig.json"))
                .collect(Collectors.toList());

        if(allReloadedMods.isEmpty() && allTTMMMods.isEmpty()){
            return List.of(new Mod(source.getName(), "Unknown", source.getAbsolutePath(), "?", "Locally loaded file",
                    source.getAbsolutePath(), search.getAbsolutePath(), new SimpleBooleanProperty(true), Mod.Type.RAW));
        }else{
            var mods = new ArrayList<Mod>();

            if(!allReloadedMods.isEmpty()){
                var choice = !askForPerms ? 0 : JOptionPane.showOptionDialog(null, "ReloadedII mods found, would you like to load any Redirector mods?",
                        "Reloaded Mods", JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, new String[]{"Yes", "No"}, "Yes");

                if(choice != 1) {
                    for(var modFile : allReloadedMods){
                        var mod = parseReloadedMod(modFile, source.getAbsolutePath());
                        if(mod != null) mods.add(mod);
                    }
                }
            }

            if(!allTTMMMods.isEmpty()){
                for(var modFile : allTTMMMods){
                    mods.addAll(parseTTMMFile(modFile, source.getAbsolutePath()));
                }
            }

            return mods;
        }
    }

    private static Mod parseReloadedMod(Path source, String origin){
        try (var in = new FileInputStream(source.toFile())) {
            var modDef = new JSONObject(IOUtils.toString(in, Charset.defaultCharset()));
            var id = modDef.getString("ModId");
            var name = modDef.getString("ModName");
            var version = modDef.getString("ModVersion");
            var desc = modDef.getString("ModDescription");
            var author = modDef.getString("ModAuthor");

            var deps = modDef.getJSONArray("ModDependencies");
            if(deps.toString().contains("reloaded.universal.redirector")){
                var newDir = source.getParent().toString() + "\\Redirector\\";

                System.out.println("Loading redirector mod " + id);

                return new Mod(name, author, id, version, desc, origin, newDir, new SimpleBooleanProperty(true), Mod.Type.RELOADEDII);
            }
            return null;
        } catch (IOException e) {
            System.out.println("Failed to read ReloadedII modfile " + source);
            return null;
        }
    }

    private static List<Mod> parseTTMMFile(Path source, String origin){
        try (var in = new FileInputStream(source.toFile())) {
            var jsonObj = new JSONObject(IOUtils.toString(in, Charset.defaultCharset()));
            var modList = jsonObj.getJSONArray("mods");

            var foundMods = new ArrayList<Mod>();
            for(var jobj : modList){
                var modDef = (JSONObject) jobj;
                var id = modDef.getString("modId");
                var name = modDef.getString("name");
                var version = modDef.getString("version");
                var desc = modDef.getString("description");
                var author = modDef.getString("author");
                var path = source.getParent().toString() + "\\" + modDef.getString("folder");

                System.out.println("Loading TTMM mod " + id);

                foundMods.add(new Mod(name, author, id, version, desc, origin, path, new SimpleBooleanProperty(true), Mod.Type.TT_MM));
            }

            return foundMods;
        } catch (IOException e) {
            System.out.println("Failed to read TTMM modfile " + source);
            return new ArrayList<>();
        }
    }
}
