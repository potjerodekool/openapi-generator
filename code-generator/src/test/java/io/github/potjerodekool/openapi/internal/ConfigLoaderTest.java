package io.github.potjerodekool.openapi.internal;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {

    @Test
    void loadConfig() throws IOException {
        ConfigLoader.loadConfig(new File("openapi/config.json"));
    }
}