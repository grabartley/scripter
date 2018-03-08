package com.example.graham.scripter;

/**
 * Created by Graham on 20/02/2017.
 *
 * Used to represent a JavaScript file
 */

public class ScripterScript {
    /*Script data*/
    private String code;
    private String filename;
    private String url;
    private String[] dependencies;
    private boolean enabled;

    /*Constructors*/
    public ScripterScript(String code, String filename, String url, String[] dependencies, boolean enabled) {
        this.code = code;
        this.filename = filename;
        this.url = url;
        this.dependencies = dependencies;
        this.enabled = enabled;
    }

    public ScripterScript(String code, String filename, String url, String[] dependencies) {
        this.code = code;
        this.filename = filename;
        this.url = url;
        this.dependencies = dependencies;
        //scripts are disabled by default
        this.enabled = false;
    }

    public ScripterScript(String code, String filename, String url, boolean enabled) {
        this.code = code;
        this.filename = filename;
        this.url = url;
        //assume no dependencies if none given
        this.dependencies = new String[0];
        this.enabled = enabled;
    }

    public ScripterScript(String code, String filename, String url) {
        this.code = code;
        this.filename = filename;
        this.url = url;
        //assume no dependencies if none given
        this.dependencies = new String[0];
        //scripts are disabled by default
        this.enabled = false;
    }

    /*Getters*/
    public String getCode() {
        return code;
    }

    public String getFilename() {
        return filename;
    }

    public String getUrl() {
        return url;
    }

    public String getDependencyByIndex(int index) {
        return dependencies[index];
    }

    public int getNumOfDependencies() {
        return dependencies.length;
    }

    public boolean isEnabled() {
        return enabled;
    }

    /*Setters*/
    public void setCode(String code) {
        this.code = code;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    //Overridden toString() to return the code of the script
    @Override
    public String toString() {
        return code;
    }
}
