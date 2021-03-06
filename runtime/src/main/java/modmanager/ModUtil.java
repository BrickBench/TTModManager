package modmanager;


import modmanager.ui.BottomPane;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import net.lingala.zip4j.ZipFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class ModUtil {
    public static List<Mod> loadSource(File path, boolean askForPerms) throws IOException {
        BottomPane.log("Loading source " + path.getAbsolutePath());
        var newSpace = createModSpace(path);
        if(newSpace != null){
            return searchForMods(newSpace, path, askForPerms);
        }
        BottomPane.log("Failed to find source file " + path);
        return new ArrayList<>();
    }

    private static File createModSpace(File path) throws IOException {
        if(path.isFile()) {
            var realPath = Util.getFromMainDirectory("Mods\\") + FilenameUtils.removeExtension(path.getName());
            if (path.getAbsolutePath().toLowerCase().endsWith(".zip")) {
                new ZipFile(path).extractAll(realPath);
                return new File(realPath);
            } else if (path.getAbsolutePath().toLowerCase().endsWith(".rar") || path.getAbsolutePath().toLowerCase().endsWith(".7zip")) {
                ArchiveUtil.extract(path.getAbsolutePath(), realPath);
                return new File(realPath);
            }else {
                return null;
            }
        } else if(path.isDirectory()){
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
            var modifiedFiles = getModifiedFiles(search.getAbsolutePath() + "\\");
            return List.of(new Mod(source.getName(), "Unknown", source.getAbsolutePath(), "?", "Locally loaded file",
                    source.getAbsolutePath(), search.getAbsolutePath() + "\\", new SimpleBooleanProperty(true), Mod.Type.RAW,
                    new SimpleIntegerProperty(0), modifiedFiles, List.of()));
        }else{
            var mods = new ArrayList<Mod>();

            if(!allReloadedMods.isEmpty()){
                Optional<ButtonType> result;
                if(askForPerms){
                    var choicePanel = new Alert(Alert.AlertType.CONFIRMATION, "Reloaded II mods found, would you like to load any Redirector mods?", ButtonType.YES, ButtonType.NO);
                    choicePanel.setTitle("Load Reloaded II mods?");
                    result = choicePanel.showAndWait();
                }else{
                    result = Optional.of(ButtonType.YES);
                }

                if(result.isPresent() && result.get() == ButtonType.YES) {
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
                var modifiedFiles = getModifiedFiles(newDir);
                var usableDeps = new ArrayList<String>();
                for(var dep : deps){
                    if(!((String)dep).contains("reloaded.universal")){
                        usableDeps.add((String)dep);
                    }
                }

                BottomPane.log("Loading redirector mod " + id);

                return new Mod(name, author, id, version, desc, origin, newDir, new SimpleBooleanProperty(true), Mod.Type.RELOADEDII, new SimpleIntegerProperty(0), modifiedFiles, usableDeps);
            }
            return null;
        } catch (IOException e) {
            BottomPane.log("Failed to read ReloadedII modfile " + source);
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
                var modifiedFiles = getModifiedFiles(path);
                var deps = new ArrayList<String>();
                if(modDef.has("dependencies")){
                    deps.addAll(modDef.getJSONArray("dependencies").toList().stream().map(s -> (String)s).collect(Collectors.toList()));
                }

                BottomPane.log("Loading TTMM mod " + id);

                foundMods.add(new Mod(name, author, id, version, desc, origin, path, new SimpleBooleanProperty(true), Mod.Type.TT_MM, new SimpleIntegerProperty(0), modifiedFiles, deps));
            }

            return foundMods;
        } catch (IOException e) {
            BottomPane.log("Failed to read TTMM modfile " + source);
            return new ArrayList<>();
        }
    }

    private static List<String> getModifiedFiles(String path) throws IOException {
        return Files.find(Paths.get(path),
                Integer.MAX_VALUE,
                (filePath, fileAttr) -> fileAttr.isRegularFile())
                .map(f -> f.toString().toLowerCase().replace(path.toLowerCase(), "").replace(".patch", ""))
                .collect(Collectors.toList());
    }
}
