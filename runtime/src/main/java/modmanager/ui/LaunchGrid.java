package modmanager.ui;

import modmanager.ManagerProperties;
import modmanager.ModManager;
import modmanager.TTModManager;
import modmanager.Util;
import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.stage.DirectoryChooser;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static java.awt.Desktop.*;

public class LaunchGrid extends GridPane {
    public LaunchGrid(){
        var runRow = new RowConstraints();
        runRow.setPercentHeight(50);
        runRow.setFillHeight(true);

        var runRow2 = new RowConstraints();
        runRow.setPercentHeight(50);
        runRow.setFillHeight(true);

        var column = new ColumnConstraints();
        column.setPercentWidth(70);
        column.setFillWidth(true);
        column.setHgrow(Priority.ALWAYS);

        var runColumn = new ColumnConstraints();
        runColumn.setPercentWidth(30);
        runColumn.setFillWidth(true);

        this.getRowConstraints().addAll(runRow, runRow2);
        this.getColumnConstraints().addAll(column, runColumn);

        var runButton = new Button("Apply and Run Mods");
        var applyNoRun = new Button("Apply Mods");
        var gameDir = new Button("Set Game Directory");


        runButton.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        applyNoRun.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        var commandList = new ComboBox<Tool>();
        commandList.getItems().add(new Tool("Game Instance\\LegoStarWarsSaga.exe", "Lego Star Wars: The Complete Saga", true));
        commandList.getItems().add(new Tool("Tools\\BrickBench\\run.bat", "BrickBench Level Editor", false));
        commandList.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        commandList.getSelectionModel().selectFirst();
        commandList.getSelectionModel().selectedItemProperty().addListener((opt, oldVal, newVal) -> {
            if(newVal != null){
                if(newVal.isGame){
                    runButton.setText("Apply and run mods");
                    applyNoRun.setText("Apply mods");
                }else{
                    runButton.setText("Run software");
                    applyNoRun.setText("Open folder");
                }
            }
        });

        runButton.setOnAction(a -> {
            var selection = commandList.getSelectionModel().getSelectedItem();
            if(selection.isGame){
                if(ManagerProperties.PROPERTIES.getProperty("originalInstall") == null || !new File(ManagerProperties.PROPERTIES.getProperty("originalInstall")).exists()){
                    var alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText("Failed to find install directory, please click \"Set Game Directory\" before launching");
                    alert.showAndWait();
                    return;
                }
                if(this.getConflictPrompt(ModManager.rescanConflicts(ModManager.getLoadedMods()))) {
                    runButton.setDisable(true);
                    applyNoRun.setDisable(true);
                    new Thread(() -> {
                        try {
                            TTModManager.CURRENT.applyMods();
                            runSafe(Util.getFromMainDirectory("\\Game Instance\\LEGOStarWarsSaga.exe"));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }finally {
                            Platform.runLater(() -> {
                                runButton.setDisable(false);
                                applyNoRun.setDisable(false);
                            });
                        }
                    }).start();
                }
            }else{
                this.run(true, selection.path);
            }
        });

        applyNoRun.setOnAction(a -> {
            var selection = commandList.getSelectionModel().getSelectedItem();
            if(selection.isGame){
                if(ManagerProperties.PROPERTIES.getProperty("originalInstall") == null || !new File(ManagerProperties.PROPERTIES.getProperty("originalInstall")).exists()){
                    var alert = new Alert(Alert.AlertType.ERROR);
                    alert.setContentText("Failed to find install directory, please click \"Set Game Directory\" before launching");
                    alert.showAndWait();
                    return;
                }

                if(this.getConflictPrompt(ModManager.rescanConflicts(ModManager.getLoadedMods()))){
                    runButton.setDisable(true);
                    applyNoRun.setDisable(true);
                    new Thread(() -> {
                        try{
                            TTModManager.CURRENT.applyMods();
                        }finally {
                            Platform.runLater(() -> {
                                runButton.setDisable(false);
                                applyNoRun.setDisable(false);
                            });
                        }
                    }).start();
                }
            }else{
                try {
                    getDesktop().open(new File(Util.getFromMainDirectory(selection.path)).getParentFile());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        gameDir.setOnAction(a -> {
            var chooser = new DirectoryChooser();
            var file = chooser.showDialog(TTModManager.CURRENT.stage);

            if(file != null) {
                ManagerProperties.PROPERTIES.put("originalInstall", file.getAbsolutePath());
            }
        });

        this.add(commandList, 0, 0, 1, 1);
        this.add(gameDir, 0, 1, 1, 1);

        this.add(runButton, 1, 0, 1, 1);
        this.add(applyNoRun, 1, 1, 1, 1);
    }

    public void runSafe(String path){
        Desktop desktop = Desktop.getDesktop();

        try {
            desktop.open(new File(path));
        } catch (IOException e) {
            Platform.runLater(() -> {
                var alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Failed to launch game executable: " + e.getMessage());
                alert.showAndWait();
                e.printStackTrace(); });
        }
    }

    public void run(boolean fromMainDirectory, String path, String... args){
        try {
            var file = fromMainDirectory ? new File(Util.getFromMainDirectory(path)) : new File(path);
            var commands = new ArrayList<String>();
            commands.add(file.getAbsolutePath());
            commands.addAll(List.of(args));
            new ProcessBuilder().command(commands).directory(file.getParentFile()).start();
        } catch (IOException e) {
            Platform.runLater(() -> {
                var alert = new Alert(Alert.AlertType.ERROR);
                alert.setContentText("Failed to launch game executable: " + e.getMessage());
                alert.showAndWait();
                e.printStackTrace(); });
        }
    }

    public boolean getConflictPrompt(List<ModManager.ConflictEntry> conflicts){
        if(conflicts.isEmpty()) return true;
        var conflictStr = conflicts.stream()
                .map(c -> switch (c.type()){
                    case CONFLICTING_FILES ->  c.conflicting().name() +  " conflicts with " + c.original().name() + ": " + c.conflictItems();
                    case DEPENDENT_DISABLED -> c.conflicting().name() +  " depends on " + c.original().name() + " which is disabled";
                    case MISSING_DEPENDENCY -> c.conflicting().name() +  " is missing dependencies " + c.conflictItems() ;
                }).collect(Collectors.joining(".\n"));

        var choicePanel = new Alert(Alert.AlertType.WARNING, "Mod file conflicts found: \n" + conflictStr +
                ".\n\nTTMM will try to repair any .TXT conflicts.\nContinue?", ButtonType.YES, ButtonType.CANCEL);
        choicePanel.setTitle("Mod conflicts");
        choicePanel.setResizable(true);

        return choicePanel.showAndWait().stream().anyMatch(b -> b == ButtonType.YES);
    }

    record Tool(String path, String name, boolean isGame){

        public String getName() {
            return name;
        }

        public String getPath(){
            return path;
        }

        @Override
        public String toString(){
            return name;
        }
    }
}
