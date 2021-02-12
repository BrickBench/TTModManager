package com.opengg.modmanager;

import com.opengg.modmanager.ui.BottomPane;
import javafx.scene.control.Alert;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ManagerProperties {
    public static Properties PROPERTIES;

    public static void load(){
        PROPERTIES = new Properties();
        var file = Util.getFromMainDirectory("settings.cfg");
        try {
            if(!new File(file).exists()){
                new File(file).getParentFile().mkdirs();
                new File(file).createNewFile();

                var in = new FileInputStream(file);

                PROPERTIES.load(in);
                PROPERTIES.put("outputInstall", Util.getFromMainDirectory("Game Instance"));

                in.close();

                save();
            }else{
                var in = new FileInputStream(file);

                PROPERTIES.load(in);
                PROPERTIES.replaceAll((f1, f2) -> f2.toString().replace("\\\\", "\\"));

                in.close();
            }
        } catch (IOException e) {
            var alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Failed to read settings file: " + e.getMessage());
            alert.showAndWait();
        }
    }

    public static void save(){
        try(var out = new FileOutputStream(Util.getFromMainDirectory("settings.cfg"))){
            PROPERTIES.store(out, "Settings");
        } catch (IOException e) {
            BottomPane.log("Failed to save settings file");
        }
    }
}
