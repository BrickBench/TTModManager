package modmanager;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.filechooser.FileSystemView;

import org.apache.commons.lang3.SystemUtils;

import modmanager.ui.BottomPane;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class Util {
    public static Optional<Path> openFileDialog(String defaultPath, String filter, LoadType type, boolean useNative){
        if(useNative){
            var dialog = new FileDialog((Frame) null);
            dialog.setDirectory(defaultPath);
            dialog.setMode(FileDialog.LOAD);
            dialog.setVisible(true);
            return Optional.ofNullable(dialog.getFile()).map(Path::of);
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
                return Optional.of(chooser.getSelectedFile().toPath());
            }else{
                return Optional.empty();
            }
        }
    }

   public static void generateLinkTree(Path source, Path dest, boolean isSymLink, String... excludedExtensions){
        try  {
            Files.list(source).forEach(child -> { 
                try {
                    if(Files.isRegularFile(child)){
                        if(Arrays.stream(excludedExtensions).noneMatch(e -> child.toString().toLowerCase().endsWith(e))){
                            if(isSymLink){
                                Files.createSymbolicLink(dest.resolve(child.getFileName()), child);
                            }else{
                                Files.createLink(dest.resolve(child.getFileName()), child);
                            }
                        }
                    }else if(Files.isDirectory(child)){
                        var newDir = dest.resolve(child.getFileName());
                        Files.createDirectories(newDir);
                        generateLinkTree(child, newDir, isSymLink, excludedExtensions);
                    }
                } catch (IOException e) { 
                    BottomPane.log("Failed creating symlink:" + e.getMessage());
                }
            });
        } catch (IOException e) {
            BottomPane.log("Failed creating symlink:" + e.getMessage());
        }
    }

    public static Path getFromMainDirectory(String path) {
        return getFromMainDirectory(Path.of(path));
    }

    public static Path getFromMainDirectory(Path path){
        if (SystemUtils.IS_OS_LINUX) {
            var xdgHome = Objects.requireNonNullElse(System.getenv("XDG_DATA_HOME"), System.getenv("HOME") + "/.local/share/");
            return Path.of(xdgHome).resolve("ttmodmanager").resolve(path);
        } else {
            return FileSystemView.getFileSystemView().getDefaultDirectory().toPath().resolve("TT Mod Manager").resolve(path);
        }
    }

    public static void deleteDir(Path path) {
        try {
            if (Files.isDirectory(path)) {
                Files.list(path).forEach(Util::deleteDir);
            } else {
                Files.delete(path);
            }
        } catch (IOException e) {
            BottomPane.log("Failed deleting symlink: " + e.getMessage());
        } 
    }

    public enum LoadType{FILE, DIRECTORY, BOTH}
}
