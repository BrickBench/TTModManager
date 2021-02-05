package com.opengg.modmanager.ui;

import com.opengg.modmanager.ManagerProperties;
import com.opengg.modmanager.Mod;
import com.opengg.modmanager.TTModManager;
import javafx.geometry.VPos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RightPane extends BorderPane {
    private final ListView<String> sourcesList;

    public RightPane(){

        var launchGrid = new LaunchGrid();

        this.setTop(launchGrid);

        var center = new VBox();
        sourcesList = new ListView<>();

        center.getChildren().add(new Label("Sources"));
        center.getChildren().add(new ScrollPane(sourcesList));

        this.setCenter(center);

        var addModButton = new Button("Add folder");
        addModButton.setOnAction(a -> {
            var chooser = new DirectoryChooser();
            chooser.setInitialDirectory(new File(ManagerProperties.PROPERTIES.getProperty("originalInstall")));
            var file = chooser.showDialog(TTModManager.CURRENT.stage);

            if(file != null) TTModManager.CURRENT.addNewMod(file);
        });
        addModButton.setTooltip(new Tooltip("Add a mod source to the mod list."));

        var addModArchiveButton = new Button("Add archive");
        addModArchiveButton.setOnAction(a -> {
            var chooser = new FileChooser();
            chooser.setInitialDirectory(new File(ManagerProperties.PROPERTIES.getProperty("originalInstall")));
            var file = chooser.showOpenDialog(TTModManager.CURRENT.stage);

            if(file != null) TTModManager.CURRENT.addNewMod(file);
        });
        addModArchiveButton.setTooltip(new Tooltip("Add a mod source to the mod list."));

        var removeMod = new Button("Remove");
        removeMod.setOnAction(a -> {
            var source = sourcesList.getSelectionModel().getSelectedItem();
            if(source != null){
                System.out.println("Removing mod source " + source);
                TTModManager.CURRENT.getLoadedMods().removeIf(m -> m.sourceFile().equals(source));
                TTModManager.CURRENT.modTable.setModList(TTModManager.CURRENT.getLoadedMods());
                TTModManager.CURRENT.writeModList();
                this.refreshSourceList(TTModManager.CURRENT.getLoadedMods());
            }
        });
        removeMod.setTooltip(new Tooltip("Removes the selected mod source from the mod list."));

        var bottom = new HBox();
        bottom.getChildren().add(addModButton);
        bottom.getChildren().add(addModArchiveButton);
        bottom.getChildren().add(removeMod);

        this.setBottom(bottom);
    }

    public void refreshSourceList(List<Mod> mods){
        var sources = mods.stream().map(Mod::sourceFile).distinct().collect(Collectors.toList());
        sourcesList.getItems().clear();
        sourcesList.getItems().addAll(sources);
    }


}
