package io.github.potjerodekool.openapi;

import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.openapi.dependency.Artifact;
import io.github.potjerodekool.openapi.dependency.Dependency;
import io.github.potjerodekool.openapi.dependency.DependencyChecker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;
import java.util.stream.Stream;

class OpenApiParserHelperIT {

    private final Set<Dependency> dependencies = Set.of(
            new Dependency("org.hibernate.validator", "hibernate-validator"),
            new Dependency("org.checkerframework", "checker-qual")
    );

    private final File generatedSourcesDir = new File("target/generated-sources");

    @AfterEach
    void tearDown() {
        emptyDir(generatedSourcesDir);
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

    @Disabled("resolve classpath")
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
                true,
                new HashMap<>()
        );

        final var project = new Project(
                new File("").toPath(),
                List.of(),
                List.of(generatedSourcesDir.toPath()),
                new File("").toPath(),
                dependencyChecker());

        new Generator().generate(
                project,
                List.of(apiConfiguration),
                Map.of(),
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
                final var classPath = System.getProperty("java.class.path")
                        .split(File.pathSeparator);

                final List<Artifact> artifacts = new ArrayList<>();

                for (String classPathElement : classPath) {
                    final var file = new File(classPathElement);
                    final var artifact = new Artifact(
                            "",
                            "",
                            file,
                            "",
                            ""
                    );
                    artifacts.add(artifact);
                }
                return artifacts.stream();
            }

        };
    }
}