package com.opengg.modmanager;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public record Mod(String name, String author, String id, String version, String description, String sourceFile, String rootPath, BooleanProperty loaded, Type type) {

    public boolean isLoaded() {
        return loaded.getValue();
    }

    public void setLoaded(boolean loaded) {
        this.loaded.setValue(loaded);
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

    public Type getType() {
        return type;
    }

    public enum Type{
        TT_MM, RELOADEDII, RAW
    }
}


