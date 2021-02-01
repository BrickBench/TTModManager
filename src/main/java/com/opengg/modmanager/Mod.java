package com.opengg.modmanager;

public class Mod {
    private String path;
    private ModType type;
    private boolean loaded;

    public Mod(String path, ModType type, boolean loaded) {
        this.path = path;
        this.loaded = loaded;
        this.type = type;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public boolean isLoaded() {
        return loaded;
    }

    public void setLoaded(boolean loaded) {
        this.loaded = loaded;
    }

    public ModType getType() {
        return type;
    }

    public void setType(ModType type) {
        this.type = type;
    }

    public enum ModType{
        ZIP, FOLDER
    }
}
