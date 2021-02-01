package com.opengg.modmanager;

import javax.swing.*;
import java.awt.*;
import java.io.File;
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
            chooser.setCurrentDirectory(new File(defaultPath));
            int returnVal = chooser.showOpenDialog(null);
            if(returnVal == JFileChooser.APPROVE_OPTION){
                return Optional.of(chooser.getSelectedFile());
            }else{
                return Optional.empty();
            }
        }
    }


    public enum LoadType{FILE, DIRECTORY, BOTH}
}
