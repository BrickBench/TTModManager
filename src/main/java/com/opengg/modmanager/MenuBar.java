package com.opengg.modmanager;

import javax.swing.*;

public class MenuBar extends JPanel {
    public MenuBar(){
        this.setBorder(BorderFactory.createEtchedBorder());

        var addModButton = new JButton("Add mod");
        addModButton.addActionListener(a -> {
            var file = Util.openFileDialog("", "", Util.LoadType.BOTH, false);
            file.ifPresent(f -> TTModManager.CURRENT.addNewMod(f));
        });
        addModButton.setToolTipText("Add a mod to the mod list.");

        var removeMod = new JButton("Remove mod");
        removeMod.addActionListener(a -> {
            var mod = TTModManager.CURRENT.modTable.getSelectedMod();
            if(mod != null){
                TTModManager.CURRENT.getLoadedMods().remove(mod);
                TTModManager.CURRENT.writeModList();
                TTModManager.CURRENT.refreshModList();
            }
        });
        removeMod.setToolTipText("Removes the selected mod from the mod list.");

        var createGameInstance = new JButton("Apply mods");
        createGameInstance.addActionListener(a -> {
            new Thread(() -> TTModManager.CURRENT.applyMods()).start();
        });
        addModButton.setToolTipText("Create a game instance based off the mod list.");

        var runGameInstance = new JButton("Run mods");
        runGameInstance.addActionListener(a -> TTModManager.CURRENT.runMod());
        addModButton.setToolTipText("Run the modded game instance");

        this.add(addModButton);
        this.add(removeMod);
        this.add(createGameInstance);
        this.add(runGameInstance);
    }
}
