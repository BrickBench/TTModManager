package com.opengg.modmanager;

import javax.swing.*;

public class TopBar extends JMenuBar {
    public TopBar(){
        var fileMenu = new JMenu();
        fileMenu.setText("File");
        this.add(fileMenu);

        var setGameDirectory = new JMenuItem("Set original game directory");
        setGameDirectory.addActionListener(a -> {
            var file = Util.openFileDialog(ManagerProperties.PROPERTIES.getProperty("originalInstall"), "", Util.LoadType.BOTH, false);
            file.ifPresent(f -> ManagerProperties.PROPERTIES.setProperty("originalInstall", f.getAbsolutePath()));
        });
        setGameDirectory.setToolTipText("Sets the base game directory for the game to mod.");

        var setModdedGameDirectory = new JMenuItem("Set modded game directory");
        setModdedGameDirectory.addActionListener(a -> {
            var file = Util.openFileDialog(ManagerProperties.PROPERTIES.getProperty("outputInstall"), "", Util.LoadType.BOTH, false);
            file.ifPresent(f -> ManagerProperties.PROPERTIES.setProperty("outputInstall", f.getAbsolutePath()));
        });
        setModdedGameDirectory.setToolTipText("Sets the directory for TTMM to install the modded game instance to.");

        fileMenu.add(setGameDirectory);
        //fileMenu.add(setModdedGameDirectory);

    }
}
