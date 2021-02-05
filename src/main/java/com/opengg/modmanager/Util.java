package com.opengg.modmanager;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.File;
import java.util.Objects;
import java.util.Optional;

public class Util {
    public static Optional<File> openFileDialog(String defaultPath, String filter, LoadType type, boolean useNative){
        if(useNative){
            var dialog = new FileDialog((Frame) null);
            dialog.setDirectory(defaultPath);
            dialog.setMode(FileDialog.LOAD);
            dialog.setVisible(true);
            return Optional.ofNullable(dialog.getFile()).map(File::new);
        }else{
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(switch (type){
                case DIRECTORY -> JFileChooser.DIRECTORIES_ONLY;
                case FILE -> JFileChooser.FILES_ONLY;
                case BOTH -> JFileChooser.FILES_AND_DIRECTORIES;
            });
            chooser.setCurrentDirectory(new File(Objects.requireNonNullElse(defaultPath, "")));
            if(!filter.isEmpty()) chooser.setFileFilter(new FileNameExtensionFilter("Mods", filter));
            int returnVal = chooser.showOpenDialog(null);
            if(returnVal == JFileChooser.APPROVE_OPTION){
                return Optional.of(chooser.getSelectedFile());
            }else{
                return Optional.empty();
            }
        }
    }

    public static String getFromMainDirectory(String path){
        return FileSystemView.getFileSystemView().getDefaultDirectory().getPath() + "\\TT Mod Manager\\" + path;
    }

    public static void deleteDir(File file) {
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                deleteDir(f);
            }
        }
        file.delete();
    }

    public enum LoadType{FILE, DIRECTORY, BOTH}
}
