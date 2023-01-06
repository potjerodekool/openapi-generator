package io.github.potjerodekool.openapi.internal;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public final class ConfigLoader {

    private ConfigLoader() {
    }

    public static void loadConfig(final File file) throws IOException {
        if (file.exists()) {
            final var config = new ObjectMapper().readValue(file, ConfigFile.class);
            System.out.println(config);
        }
    }
}
