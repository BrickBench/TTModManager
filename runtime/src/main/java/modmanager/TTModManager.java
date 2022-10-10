package modmanager;

import modmanager.patching.TextPatcher;
import modmanager.ui.BottomPane;
import modmanager.ui.ModTable;
import modmanager.ui.SourcePane;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

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
    public SourcePane rightPane;

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

        rightPane = new SourcePane();
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

        BottomPane.log("Attempting character repairs when applying mods");
        var sourceDir = Path.of(ManagerProperties.PROPERTIES.getProperty("originalInstall"));
        var dstDir = Path.of(ManagerProperties.PROPERTIES.getProperty("outputInstall"));
        var backupMods = List.copyOf(ModManager.getLoadedMods());

        if(!Files.exists(sourceDir)){
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
            Util.deleteDir(dstDir);
            BottomPane.log("Old instance removed");

            bottomPane.setProgress(1  / maxSize);
            bottomPane.setProgressString("Creating new game instance...");

            BottomPane.log(sourceDir.toString());
            BottomPane.log(dstDir.toString());

            Util.generateLinkTree(sourceDir, dstDir, SystemUtils.IS_OS_LINUX, "ai.pal",  "hat_hair_all_pc.gsc", "head_all_pc.gsc");

        } catch (Exception e) {
            Platform.runLater(() -> {
                var alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Failed to copy original game to modded game directory: " + e.getMessage());
                alert.showAndWait();
                e.printStackTrace();
            });
            return;
        }

        bottomPane.setProgress(2 / maxSize);

        var progress = 2;

        for(var mod : backupMods.stream().filter(Mod::isEnabled).collect(Collectors.toList())){
            try {
                applyMod(sourceDir, dstDir, mod);
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

    private void applyMod(Path sourceDir, Path dstDir, Mod mod) throws IOException {
        BottomPane.log("Copying mod in place: " + mod.id());
        bottomPane.setProgressString("Copying mod " + mod.rootPath());

        var softwareMergedFiles = List.of(Path.of("chars/chars.txt"), Path.of("chars/collection.txt"), Path.of("stuff/text/english.txt"));

        for(var textFile : softwareMergedFiles){
            if(mod.editedFiles().contains(textFile)){
                Files.move(mod.rootPath().resolve(textFile), mod.rootPath().resolve(textFile + ".temp"));
            }
        }

        FileUtils.copyDirectory(mod.rootPath().toFile(), dstDir.toFile());

        var patchState = new TextPatcher.PatcherState();

        for(var textFile : softwareMergedFiles){
            if(mod.editedFiles().contains(textFile)){
                if(Files.isSymbolicLink(dstDir.resolve(textFile))){
                    Files.copy(sourceDir.resolve(textFile), dstDir.resolve(textFile), StandardCopyOption.REPLACE_EXISTING);
                }

                Files.move(mod.rootPath().resolve(textFile + ".temp"), mod.rootPath().resolve(textFile));
                TextPatcher.patchTextFile(mod.rootPath().resolve(textFile), dstDir.resolve(textFile), sourceDir.resolve(textFile), patchState);
            }
        }

        var allPatches = Files.find(dstDir,
                Integer.MAX_VALUE,
                (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.toString().toLowerCase().endsWith("patch"))
                .collect(Collectors.toList());

        bottomPane.setProgressString("Applying patches for " + mod.name());
        BottomPane.log("Applying patches for: " + mod.id());

        for(var patch : allPatches){
            var equivalentFile = Path.of(patch.toString().replace(".patch", ""));
            var localEquivalent = equivalentFile.toString().replace(dstDir.toString(), "");

            if(Files.readAttributes(equivalentFile, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).isSymbolicLink()){
                bottomPane.setProgressString("Copying file " + localEquivalent);
                Files.copy(sourceDir.resolve(localEquivalent), dstDir.resolve(localEquivalent), StandardCopyOption.REPLACE_EXISTING);
            }

            if(Files.exists(equivalentFile)){
            //    JBPatch.bspatch(equivalentFile.toFile(), equivalentFile.toFile(), patch.toFile());
                Files.delete(patch);
            }
        }
    }
}
