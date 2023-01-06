package io.github.potjerodekool.openapi.gradle;

import io.github.potjerodekool.openapi.dependency.Artifact;
import io.github.potjerodekool.openapi.dependency.DependencyChecker;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ResolvedArtifact;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

public class GradleDependencyChecker implements DependencyChecker {

    private final Project project;

    public GradleDependencyChecker(final Project project) {
        this.project = project;
    }

    private Configuration getConfiguration() {
        return project.getConfigurations().getByName("runtimeClasspath");
    }

    @Override
    public boolean isDependencyPresent(final String groupId, final String artifactId) {
        final var config = getConfiguration();
        return config.getDependencies().stream()
                .anyMatch(it ->
                                Objects.equals(it.getGroup(), groupId)
                                && Objects.equals(it.getName(), artifactId)
                );
    }

    @Override
    public boolean isClassPresent(final String className) {
        final var classFileName = className.replace('.', '/') + ".class";
        return getConfiguration().getFiles().stream()
                .filter(file -> file.getName().endsWith(".jar"))
                .anyMatch(file -> isClassPresent(file, classFileName));
    }

    private boolean isClassPresent(final File file,
                                   final String classFileName) {
        try(final ZipFile zipFile = new ZipFile(file)) {
            final var entry = zipFile.getEntry(classFileName);
            return entry != null;
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public Stream<Artifact> getProjectArtifacts() {
        return getConfiguration().getResolvedConfiguration().getResolvedArtifacts().stream()
                .map(this::toArtifact);
    }

    private Artifact toArtifact(final ResolvedArtifact resolvedArtifact) {
        final var displayName = resolvedArtifact.getId().getComponentIdentifier().getDisplayName();
        final var elements = displayName.split(":");
        final var groupId = elements[0];
        final var artifactId = elements[1];

        return new Artifact(
                groupId,
                artifactId,
                resolvedArtifact.getFile(),
                resolvedArtifact.getClassifier(),
                resolvedArtifact.getType()
        );
    }
}
