package io.github.potjerodekool.openapi.internal;

import java.util.HashMap;
import java.util.Map;

public class ConfigFile {

    private Map<String, String> controllers = new HashMap<>();

    public Map<String, String> getControllers() {
        return controllers;
    }

    public void setControllers(final Map<String, String> controllers) {
        this.controllers = controllers;
    }
}


