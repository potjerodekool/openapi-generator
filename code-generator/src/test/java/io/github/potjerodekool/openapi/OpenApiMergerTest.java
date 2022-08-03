package io.github.potjerodekool.openapi;

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
        // "petstore/petstore.yaml"
        //final var apiFile = new File("openapi/spec.yml");

        //final var apiFile = new File("auth/openapi/spec.yaml");
        final var apiFile = new File("C:\\projects\\auth-server\\openapi\\spec.yaml");

        final var config = new OpenApiGeneratorConfig(
                apiFile,
                new File("target/generated-sources"),
                "org.some.config"
        );
        config.setAddCheckerAnnotations(true);

        new Generator().generate(config, this.dependencyChecker());
    }

    private DependencyChecker dependencyChecker() {
        return (groupId1, artifactId1) -> false;
    }
}