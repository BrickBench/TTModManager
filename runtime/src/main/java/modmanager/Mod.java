package modmanager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;

import java.util.List;


//Represents a mod
public record Mod(String name, String author, String id, String version, String description, String sourceFile,
                  String rootPath, BooleanProperty enabled, Type type, IntegerProperty modOrder, List<String> editedFiles, List<String> dependencies) {

    public boolean isEnabled() {
        return enabled.getValue();
    }

    public void setEnabled(boolean loaded) {
        this.enabled.setValue(loaded);
    }

    public int getModOrder() {
        return modOrder.getValue();
    }

    public enum Type{
        TT_MM, RELOADEDII, RAW
    }
}


