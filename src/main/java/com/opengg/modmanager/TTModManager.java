package com.opengg.modmanager;

import jbdiff.JBPatch;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.stream.Collectors;

public class TTModManager extends JFrame {
    public static TTModManager CURRENT;

    public ModTable modTable;
    public BottomBar bottomBar;

    private List<Mod> modList;

    public static void main(String... args){
        var manager = new TTModManager();
    }

    public TTModManager(){
        CURRENT = this;
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

        ManagerProperties.load();

        this.setLayout(new BorderLayout());
        this.setJMenuBar(new TopBar());

        this.add(new MenuBar(), BorderLayout.NORTH);
        this.add(new JScrollPane(modTable = new ModTable()), BorderLayout.CENTER);
        this.add(bottomBar = new BottomBar(), BorderLayout.SOUTH);

        this.setMinimumSize(new Dimension(600,400));
        this.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ModListFileManager.writeModList(modList);
                ManagerProperties.save();
                System.exit(0);
            }
        });
        this.setTitle("TT Mod Manager");
        this.setVisible(true);

        refreshModList();
    }

    public void addNewMod(File modFile){
        if(modList.stream().anyMatch(m -> m.getPath().equalsIgnoreCase(modFile.getAbsolutePath()))) return;

        Mod mod;
        if(modFile.isDirectory()){
            mod = new Mod(modFile.getPath(), Mod.ModType.FOLDER, true);
        }else{
            mod = new Mod(modFile.getPath(), Mod.ModType.FOLDER, true);
        }

        modList.add(mod);
        writeModList();
        refreshModList();
    }

    public void writeModList(){
        ModListFileManager.writeModList(modList);
    }

    public void refreshModList(){
        modList = ModListFileManager.readModList();
        modTable.setModList(modList);
    }

    public List<Mod> getLoadedMods(){
        return modList;
    }

    public void applyMods(){
        ModListFileManager.writeModList(modList);

        var sourceDir = ManagerProperties.PROPERTIES.getProperty("originalInstall");
        var dstDir = ManagerProperties.PROPERTIES.getProperty("outputInstall");
        var backupMods = List.copyOf(modList);

        if(!new File(sourceDir).exists()){
            JOptionPane.showMessageDialog(this, "The game directory does not exist, cannot apply patches.");
            return;
        }

        bottomBar.setProgress(0);
        bottomBar.setProgressMax(modList.size() + 2);

        try {
            bottomBar.setProgressString("Deleting old instance...");
            FileCopier.deleteDir(new File(dstDir));

            bottomBar.setProgress(1);
            bottomBar.setProgressString("Creating new game instance...");
            FileCopier.copyFileOrFolder(new File(sourceDir), new File(dstDir), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to copy original game to modded game directory: " + e.getMessage());
            return;
        }

        bottomBar.setProgress(2);

        var progress = 2;

        for(var mod : backupMods.stream().filter(Mod::isLoaded).collect(Collectors.toList())){
            try {
                bottomBar.setProgressString("Copying mod " + mod.getPath());
                FileCopier.copyFileOrFolder(new File(mod.getPath()), new File(dstDir), StandardCopyOption.REPLACE_EXISTING);
                bottomBar.setProgress(++progress);

                bottomBar.setProgressString("Applying patches for " + mod.getPath());
                var allPatches = Files.find(Paths.get(dstDir),
                        Integer.MAX_VALUE,
                        (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.toString().toLowerCase().endsWith("patch"))
                        .collect(Collectors.toList());

                for(var patch : allPatches){
                    var equivalentFile = new File(patch.toString().toLowerCase().replace(".patch", ""));
                    if(equivalentFile.exists()){
                        JBPatch.bspatch(equivalentFile, equivalentFile, patch.toFile());
                        Files.delete(patch);
                    }
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Failed to apply mod " + mod.getPath() + ": " + e.getMessage());
                return;
            }
        }

        bottomBar.setProgressString("Created new game instance");

    }

    public void runMod(){
        try {
            Runtime.getRuntime().exec("F:\\LEGO Files\\MODDINGOUT\\LEGOStarWarsSaga.exe", null, new File("F:\\LEGO Files\\MODDINGOUT"));
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Failed to launch game executable");
        }
    }
}
