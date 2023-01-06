package io.github.potjerodekool.openapi;

import io.github.potjerodekool.openapi.dependency.Artifact;
import io.github.potjerodekool.openapi.dependency.Dependency;
import io.github.potjerodekool.openapi.dependency.DependencyChecker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

class OpenApiParserHelperIT {

    private final Set<Dependency> dependencies = Set.of(
            new Dependency("org.hibernate.validator", "hibernate-validator"),
            new Dependency("org.checkerframework", "checker-qual")
    );

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
        //final var apiFile = new File("../demo/petstore/petstore.yaml");
        //final var apiFile = new File("openapi/spec.yml");
        //final var apiFile = new File("C:\\projects\\openapi-generator\\demo\\external\\api.json");
        final var apiFile = new File("C:\\projects\\auth-server2\\authserver\\openapi\\spec.yml");

        final var apiConfiguration = new ApiConfiguration(
                apiFile,
                "org.some",
                true,
                true,
                new HashMap<>()
        );

        final var project = new Project(
                new File("").toPath(),
                List.of(),
                List.of(),
                new File("").toPath(),
                dependencyChecker());

        new Generator().generate(
                project,
                List.of(apiConfiguration),
                new HashMap<>(),
                "org.some",
                Language.JAVA
        );
    }

    private DependencyChecker dependencyChecker() {
        return new DependencyChecker() {
            @Override
            public boolean isDependencyPresent(final String groupId,
                                               final String artifactId) {
                return dependencies.contains(new Dependency(groupId, artifactId));
            }

            @Override
            public boolean isClassPresent(final String className) {
                return false;
            }

            @Override
            public Stream<Artifact> getProjectArtifacts() {
                return Stream.empty();
            }

        };
    }
}