package com.opengg.modmanager;

import javax.swing.*;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ManagerProperties {
    public static Properties PROPERTIES;

    public static void load(){
        PROPERTIES = new Properties();
        try(var in = new FileInputStream("settings.cfg")) {
            PROPERTIES.load(in);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(TTModManager.CURRENT, "Failed to read settings file");
        }
    }

    public static void save(){
        try(var out = new FileOutputStream("settings.cfg")){
            PROPERTIES.store(out, "Settings");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(TTModManager.CURRENT, "Failed to save settings file");
        }
    }
}
