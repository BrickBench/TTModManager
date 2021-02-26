package modmanager;

import modmanager.patching.TextPatcher;
import modmanager.ui.BottomPane;
import modmanager.ui.ModTable;
import modmanager.ui.RightPane;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import jbdiff.JBPatch;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.stream.Collectors;

public class TTModManager extends Application {
    public static TTModManager CURRENT;
    public BorderPane mainPane;
    public Stage stage;

    public ModTable modTable;
    public BottomPane bottomPane;
    public RightPane rightPane;

    public static void main(String... args){
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        CURRENT = this;
        this.stage = stage;

        ManagerProperties.load();

        mainPane = new BorderPane();
        mainPane.setPadding(new Insets(5,10,5,10));

        var tablePane = new ScrollPane(modTable = new ModTable());
        tablePane.setFitToHeight(true);
        tablePane.setFitToWidth(true);
        tablePane.setPadding(new Insets(5,5,5,5));
        tablePane.setVbarPolicy(ScrollPane.ScrollBarPolicy.ALWAYS);

        mainPane.setCenter(tablePane);
        mainPane.setBottom(bottomPane = new BottomPane());

        rightPane = new RightPane();
        mainPane.setRight(rightPane);

        mainPane.setMinSize(900,500);
        stage.setOnCloseRequest(e -> {
                ModListFileManager.writeModList(ModManager.getLoadedMods());
                ManagerProperties.save();
                System.exit(0);
            });

        Scene scene = new Scene(mainPane, 1000, 500);

        stage.setScene(scene);
        stage.setTitle("TT Mod Manager");
        stage.sizeToScene();
        stage.getIcons().add(new Image("file:lego.png"));
        stage.show();

        stage.setMinWidth(stage.getWidth());
        stage.setMinHeight(stage.getHeight() + 30);

        TTModManager.CURRENT.bottomPane.setProgressString("Loading mod file");

        ModListFileManager.readModList();
    }

    public void applyMods(){
        TextPatcher.nextCharacter = 1675;
        ModListFileManager.writeModList(ModManager.getLoadedMods());
      //  var conflicts = ModManager.rescanConflicts();

       /* var shouldRepairCharacters =
                conflicts.stream().filter(c -> c.type() == ModManager.ConflictEntry.Type.CONFLICTING_FILES)
                                  .anyMatch(c ->    c.conflictItems().contains("chars\\chars.txt") ||
                                                    c.conflictItems().contains("chars\\collection.txt") ||
                                                    c.conflictItems().contains("stuff\\text\\english.txt"));*/

       // if(shouldRepairCharacters) BottomPane.log("Attempting character repairs when applying mods");
        var shouldRepairCharacters = true;
        BottomPane.log("Attempting character repairs when applying mods");
        var sourceDir = ManagerProperties.PROPERTIES.getProperty("originalInstall");
        var dstDir = ManagerProperties.PROPERTIES.getProperty("outputInstall");
        var backupMods = List.copyOf(ModManager.getLoadedMods());

        if(!new File(sourceDir).exists()){
            var alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("The game directory does not exist, cannot apply patches.");
            alert.showAndWait();
            return;
        }

        double maxSize = ModManager.getLoadedMods().stream().filter(Mod::isEnabled).count() + 2;
        bottomPane.setProgress(0);

        try {
            bottomPane.setProgressString("Deleting old instance...");
            BottomPane.log("Deleting old game instance");
            Util.deleteDir(new File(dstDir));
            BottomPane.log("Old instance removed");

            bottomPane.setProgress(1  / maxSize);
            bottomPane.setProgressString("Creating new game instance...");

            ModApplier.createLinkTree(sourceDir, dstDir);

            BottomPane.log("Copying script files");
            Files.copy(Path.of("python38.dll"), Path.of(dstDir + "\\python38.dll"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(Path.of("TTMMScriptInjector.dll"), Path.of(dstDir + "\\TTMMScriptInjector.dll"), StandardCopyOption.REPLACE_EXISTING);
            Files.copy(Path.of("TTMMScriptingLibrary.cp38-win32.pyd"), Path.of(dstDir + "\\TTMMScriptingLibrary.cp38-win32.pyd"), StandardCopyOption.REPLACE_EXISTING);
            new File(dstDir + "\\MODSCRIPTS").mkdirs();
            Files.copy(Path.of("TTMMScriptingLibrary.cp38-win32.pyd"), Path.of(dstDir + "\\MODSCRIPTS\\TTMMScriptingLibrary.cp38-win32.pyd"), StandardCopyOption.REPLACE_EXISTING);

        } catch (IOException e) {
            Platform.runLater(() -> {
                var alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Failed to copy original game to modded game directory: " + e.getMessage());
                alert.showAndWait();
                e.printStackTrace();
            });
            return;
        }

        bottomPane.setProgressString("Deleting pakfiles");

        try {
            Files.find(Paths.get(dstDir),
                    Integer.MAX_VALUE,
                    (filePath, fileAttr) -> filePath.toString().toLowerCase().endsWith("ai.pak"))
                    .forEach(p -> p.toFile().delete());
            FileUtils.forceDelete(new File(dstDir + "\\chars\\weirdo\\all"));
            FileUtils.forceDelete(new File(dstDir + "\\alltxt.pak"));

        } catch (IOException e) {
            Platform.runLater(() -> {
                var alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Failed to delete ai.pak files: " + e.getMessage());
                alert.showAndWait();
                e.printStackTrace();
            });
            return;
        }
        bottomPane.setProgress(2 / maxSize);

        var progress = 2;

        for(var mod : backupMods.stream().filter(Mod::isEnabled).collect(Collectors.toList())){
            try {
                applyMod(sourceDir, dstDir, mod, shouldRepairCharacters);
                bottomPane.setProgress(++progress / maxSize);
            } catch (IOException e) {
                Platform.runLater(() -> {
                    var alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText("Failed to apply mod " + mod.rootPath() + ": " + e.getMessage());
                    alert.showAndWait();
                    e.printStackTrace();
                });
                return;
            }
        }

        bottomPane.setProgressString("Created new game instance");
        BottomPane.log("Finished creating game instance");
    }

    private void applyMod(String sourceDir, String dstDir, Mod mod, boolean repairCharacters) throws IOException {
        BottomPane.log("Copying mod in place: " + mod.id());

        bottomPane.setProgressString("Copying mod " + mod.rootPath());

        List<String> automatedMerge = repairCharacters ?
                List.of("chars\\chars.txt", "chars\\collection.txt", "stuff\\text\\english.txt") :
                List.of("chars\\chars.txt");

        for(var textFile : automatedMerge){
            if(mod.editedFiles().contains(textFile)){
                Files.move(Path.of(mod.rootPath() + textFile), Path.of(mod.rootPath() + textFile + ".temp"));
            }
        }

        FileUtils.copyDirectory(new File(mod.rootPath()), new File(dstDir));

        for(var textFile : automatedMerge){
            if(mod.editedFiles().contains(textFile)){
                if(Files.isSymbolicLink( Path.of(dstDir + "\\" + textFile))){
                    Files.copy(Path.of(sourceDir + "\\" + textFile), Path.of(dstDir + "\\" + textFile), StandardCopyOption.REPLACE_EXISTING);
                }
                Files.move(Path.of(mod.rootPath() + textFile + ".temp"), Path.of(mod.rootPath() + textFile));
                TextPatcher.patchTextFile(mod.rootPath() + textFile, dstDir + "\\" + textFile, sourceDir + "\\" + textFile);
            }
        }

        var allPatches = Files.find(Paths.get(dstDir),
                Integer.MAX_VALUE,
                (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.toString().toLowerCase().endsWith("patch"))
                .collect(Collectors.toList());

        bottomPane.setProgressString("Applying patches for " + mod.name());
        BottomPane.log("Applying patches for: " + mod.id());

        for(var patch : allPatches){
            var equivalentFile = new File(patch.toString().replace(".patch", ""));
            var localEquivalent = equivalentFile.toString().replace(dstDir, "");

            if(Files.readAttributes(equivalentFile.toPath(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).isSymbolicLink()){
                bottomPane.setProgressString("Copying file " + localEquivalent);
                Files.copy(Path.of(sourceDir + localEquivalent), Path.of(dstDir + localEquivalent), StandardCopyOption.REPLACE_EXISTING);
            }

            if(equivalentFile.exists()){
                JBPatch.bspatch(equivalentFile, equivalentFile, patch.toFile());
                Files.delete(patch);
            }
        }
    }
}
