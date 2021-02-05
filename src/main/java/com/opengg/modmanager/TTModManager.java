package com.opengg.modmanager;

import com.opengg.modmanager.ui.*;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import jbdiff.JBPatch;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class TTModManager extends Application {
    public static TTModManager CURRENT;
    public BorderPane mainPane;
    public Stage stage;

    public ModTable modTable;
    public BottomPane bottomPane;
    public RightPane rightPane;

    private List<Mod> modList = new ArrayList<>();

    public static void main(String... args){
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        CURRENT = this;
        this.stage = stage;

        ManagerProperties.load();

        var root = new VBox(new TopBar());

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
                ModListFileManager.writeModList(modList);
                ManagerProperties.save();
                System.exit(0);
            });

        ModListFileManager.readModList();


        root.getChildren().add(mainPane);
        Scene scene = new Scene(root, 900, 500);

        stage.setScene(scene);
        stage.setTitle("TT Mod Manager");
        stage.sizeToScene();
        stage.getIcons().add(new Image("file:lego.png"));
        stage.show();

        stage.setMinWidth(stage.getWidth());
        stage.setMinHeight(stage.getHeight() + 30);
    }

    public void addNewMod(File modFile){
        try{
            var mods = ModUtil.loadMod(modFile, true);
            for(var mod : mods){
                registerMod(mod);
            }
        }catch (Exception e){
            var alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Failed to load mods in modfile " + modFile + ": " + e.getMessage());
            alert.showAndWait();
        }
    }

    public void registerMod(Mod mod){
        if(modList.stream().anyMatch(m -> m.name().equals(mod.name()))) return;

        modList.add(mod);
        modTable.setModList(modList);
        rightPane.refreshSourceList(modList);
    }

    public void writeModList(){
        ModListFileManager.writeModList(modList);
    }

    public List<Mod> getLoadedMods(){
        return modList;
    }

    public void applyMods(boolean useSymLink){
        ModListFileManager.writeModList(modList);

        var sourceDir = ManagerProperties.PROPERTIES.getProperty("originalInstall");
        var dstDir = ManagerProperties.PROPERTIES.getProperty("outputInstall");
        var backupMods = List.copyOf(modList);
        var editedFiles = new HashSet<String>();


        if(!new File(sourceDir).exists()){
            var alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("The game directory does not exist, cannot apply patches.");
            alert.showAndWait();
            return;
        }

        double maxSize = modList.stream().filter(Mod::isLoaded).count() + 2;
        bottomPane.setProgress(0);

        try {
            bottomPane.setProgressString("Deleting old instance...");
            Platform.runLater(() -> System.out.println("Deleting old game instance"));
            Util.deleteDir(new File(dstDir));
            Platform.runLater(() -> System.out.println("Old instance removed"));

            bottomPane.setProgress(1  / maxSize);
            bottomPane.setProgressString("Creating new game instance...");
            var allFiles = Files.find(Paths.get(sourceDir),
                    Integer.MAX_VALUE,
                    (filePath, fileAttr) -> fileAttr.isRegularFile())
                    .map(f -> f.toString().replace(sourceDir, ""))
                    .collect(Collectors.toList());

            if(useSymLink){
                Platform.runLater(() -> System.out.println("Creating symlink tree"));

                for(var file : allFiles){
                    new File(dstDir + file).getParentFile().mkdirs();
                    Files.createSymbolicLink(Path.of(dstDir + file), Path.of(sourceDir + file));
                }
            }else{
                FileUtils.copyDirectory(new File(sourceDir), new File(dstDir));
            }
        } catch (IOException e) {
            var alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Failed to copy original game to modded game directory: " + e.getMessage());
            alert.showAndWait();
            e.printStackTrace();
            return;
        }

        Platform.runLater(() -> System.out.println("Symlink tree created"));

        bottomPane.setProgress(2 / maxSize);

        var progress = 2;

        for(var mod : backupMods.stream().filter(Mod::isLoaded).collect(Collectors.toList())){
            try {
                Platform.runLater(() -> System.out.println("Copying mod in place: " + mod.id()));

                bottomPane.setProgressString("Copying mod " + mod.rootPath());

                FileUtils.copyDirectory(new File(mod.rootPath()), new File(dstDir));

                Files.find(Paths.get(dstDir),
                        Integer.MAX_VALUE,
                        (filePath, fileAttr) -> fileAttr.isRegularFile() && !filePath.toString().toLowerCase().endsWith("patch"))
                        .forEach(p -> editedFiles.add(p.toString()));

                var allPatches = Files.find(Paths.get(dstDir),
                        Integer.MAX_VALUE,
                        (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.toString().toLowerCase().endsWith("patch"))
                        .collect(Collectors.toList());

                bottomPane.setProgressString("Applying patches for " + mod.name());
                Platform.runLater(() -> System.out.println("Applying patches for: " + mod.id()));

                for(var patch : allPatches){
                    var equivalentFile = new File(patch.toString().replace(".patch", ""));
                    var localEquivalent = equivalentFile.toString().replace(dstDir, "");

                    if(editedFiles.contains(equivalentFile.toString()))
                        throw new UnsupportedOperationException("Mod " + mod.rootPath() + " attempted to modify" + equivalentFile + " which has already been edited!");

                    if(Files.readAttributes(equivalentFile.toPath(), BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS).isSymbolicLink()){
                        bottomPane.setProgressString("Copying file " + localEquivalent);
                        Files.copy(Path.of(sourceDir + localEquivalent), Path.of(dstDir + localEquivalent), StandardCopyOption.REPLACE_EXISTING);
                    }

                    if(equivalentFile.exists()){
                        editedFiles.add(equivalentFile.toString());
                        JBPatch.bspatch(equivalentFile, equivalentFile, patch.toFile());
                        Files.delete(patch);
                    }
                }

                bottomPane.setProgress(++progress / maxSize);
            } catch (IOException e) {
                var alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Failed to apply mod " + mod.rootPath() + ": " + e.getMessage());
                alert.showAndWait();
                e.printStackTrace();
                return;
            }
        }

        bottomPane.setProgressString("Created new game instance");
        Platform.runLater(() -> System.out.println("Finished creating game instance"));
    }
}
