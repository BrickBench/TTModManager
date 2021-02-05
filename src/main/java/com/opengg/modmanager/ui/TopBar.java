package com.opengg.modmanager.ui;

import com.opengg.modmanager.ManagerProperties;
import com.opengg.modmanager.TTModManager;
import javafx.scene.control.*;
import javafx.stage.FileChooser;

import java.io.File;

public class TopBar extends MenuBar {
    public TopBar(){
        var fileMenu = new Menu();
        fileMenu.setText("File");
        this.getMenus().add(fileMenu);

        var setGameDirectory = new CustomMenuItem(new Label("Set original game directory"));
        setGameDirectory.setOnAction(a -> {
            var chooser = new FileChooser();
            chooser.setInitialDirectory(new File(ManagerProperties.PROPERTIES.getProperty("originalInstall")));
            var file = chooser.showOpenDialog(TTModManager.CURRENT.stage);

            if(file != null) ManagerProperties.PROPERTIES.setProperty("originalInstall", file.getAbsolutePath());
            ManagerProperties.save();
        });
        Tooltip.install(setGameDirectory.getContent(), new Tooltip("Sets the base game directory for the game to mod."));
        fileMenu.getItems().add(setGameDirectory);
    }
}
