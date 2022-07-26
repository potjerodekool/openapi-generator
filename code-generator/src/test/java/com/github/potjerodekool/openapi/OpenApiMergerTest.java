package com.github.potjerodekool.openapi;

import com.github.javaparser.StaticJavaParser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;

class OpenApiMergerTest {

    @AfterEach
    void tearDown() {
        emptyDir(new File("target/generated-sources"));
    }

    private void emptyDir(final File dir) {
        final var files = dir.listFiles();

        if (files != null) {
            for (final var file : files) {
                if (file.isDirectory()) {
                    emptyDir(file);
                }
                file.delete();
            }
        }
    }

    @Test
    void merge() {
        final var code = StaticJavaParser.parse("""
                public class Test {
                    char c = 'c';
                    byte b = 0xa;
                }                
                """);

        //final var apiFile = new File("api/spec.yaml");
        final var apiFile = new File("openapi/spec.yml");

        final var config = new OpenApiGeneratorConfig(
                apiFile,
                new File("target/generated-sources")
        );
        config.setAddCheckerAnnotations(true);

        new Generator().generate(config);
    }
}