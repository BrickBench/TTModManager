package modmanager;

import modmanager.ui.BottomPane;
import javafx.scene.control.Alert;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Properties;

public class ManagerProperties {
    public static Properties PROPERTIES;

    public static void load(){
        PROPERTIES = new Properties();
        var file = Util.getFromMainDirectory("settings.cfg");
        try {
            if(!Files.exists(file)){
                Files.createDirectories(file.getParent());
                Files.createFile(file);

                PROPERTIES.put("outputInstall", Util.getFromMainDirectory("Game Instance").toString());

                save();
            }else{
                try(var in = new FileInputStream(file.toFile())) {
                    PROPERTIES.load(in);
                    PROPERTIES.replaceAll((f1, f2) -> f2.toString().replace("\\\\", "\\"));
                }
            }
        } catch (IOException e) {
            var alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Failed to read settings file: " + e.getMessage());
            alert.showAndWait();
        }
    }

    public static void save(){
        try(var out = new FileOutputStream(Util.getFromMainDirectory("settings.cfg").toFile())){
            PROPERTIES.store(out, "Settings");
        } catch (IOException e) {
            BottomPane.log("Failed to save settings file");
        }
    }
}
