package modmanager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.IntegerProperty;

import java.util.List;

public record Mod(String name, String author, String id, String version, String description, String sourceFile,
                  String rootPath, BooleanProperty enabled, Type type, IntegerProperty modOrder, List<String> editedFiles, List<String> dependencies) {

    public boolean isEnabled() {
        return enabled.getValue();
    }

    public void setEnabled(boolean loaded) {
        this.enabled.setValue(loaded);
    }

    public String getName() {
        return name;
    }

    public String getAuthor() {
        return author;
    }

    public String getId() {
        return id;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public String getRootPath() {
        return rootPath;
    }

    public int getModOrder() {
        return modOrder.getValue();
    }

    public Type getType() {
        return type;
    }

    public enum Type{
        TT_MM, RELOADEDII, RAW
    }
}


