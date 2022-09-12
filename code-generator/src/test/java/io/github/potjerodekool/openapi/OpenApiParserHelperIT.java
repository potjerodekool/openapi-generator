package io.github.potjerodekool.openapi;

import io.github.potjerodekool.openapi.dependency.Artifact;
import io.github.potjerodekool.openapi.dependency.Dependency;
import io.github.potjerodekool.openapi.dependency.DependencyChecker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;
import java.util.stream.Stream;

class OpenApiParserHelperIT {

    private final Set<Dependency> dependencies = Set.of(new Dependency(
            "org.hibernate.validator",
            "hibernate-validator"
    ));

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
        final var apiFile = new File("../demo/petstore/petstore.yaml");

        final var config = OpenApiGeneratorConfig.createBuilder(
                apiFile,
                new File("target/generated-sources"),
                "org.some.config",
                null
                ).language(Language.KOTLIN)
                .build(ConfigType.DEFAULT);

        new Generator().generate(config, this.dependencyChecker());
    }

    private DependencyChecker dependencyChecker() {
        return new DependencyChecker() {
            @Override
            public boolean isDependencyPresent(final String groupId,
                                               final String artifactId) {
                return dependencies.contains(new Dependency(groupId, artifactId));
            }

            @Override
            public Stream<Artifact> getProjectArtifacts() {
                return Stream.empty();
            }

        };
    }
}