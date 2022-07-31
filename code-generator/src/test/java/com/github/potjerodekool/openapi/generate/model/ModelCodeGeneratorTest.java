package com.github.potjerodekool.openapi.generate.model;

import com.github.potjerodekool.openapi.Filer;
import com.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class ModelCodeGeneratorTest {

    private final File outputDir = new File("target/ut-generated-sources");

    @AfterEach
    void tearDown() {
    }

    @Test
    void generate() {
        final var filter = Mockito.mock(Filer.class);

        final var config = new OpenApiGeneratorConfig(
                new File("test.yaml"),
                outputDir
        );

        final var generator = new ModelCodeGenerator(
                config,
                filter
        );

    }
}