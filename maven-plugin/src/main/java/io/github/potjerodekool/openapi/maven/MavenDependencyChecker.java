package io.github.potjerodekool.openapi.maven;

import io.github.potjerodekool.openapi.dependency.Artifact;
import io.github.potjerodekool.openapi.dependency.DependencyChecker;
import io.github.potjerodekool.openapi.log.Logger;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;
import java.util.zip.ZipFile;

public class MavenDependencyChecker implements DependencyChecker {

    private static final Logger LOGGER = Logger.getLogger(MavenDependencyChecker.class.getName());

    private static final Set<String> SCOPES = Set.of("compile", "runtime");

    private final MavenProject project;

    public MavenDependencyChecker(final MavenProject project) {
        this.project = project;
    }

    @Override
    public boolean isDependencyPresent(final String groupId,
                                       final String artifactId) {
        return project.getDependencies().stream()
                .filter(dependency -> SCOPES.contains(dependency.getScope()))
                .anyMatch(dependency -> groupId.equals(dependency.getGroupId())
                        && artifactId.equals(dependency.getArtifactId()));
    }

    @Override
    public boolean isClassPresent(final String className) {
        final var classFileName = className.replace('.', '/') + ".class";

        return project.getArtifacts().stream()
                .filter(artifact -> SCOPES.contains(artifact.getScope()))
                .filter(artifact -> "jar".equals(artifact.getType()))
                .filter(artifact -> artifact.getFile() != null)
                .anyMatch(artifact -> isClassPresent(artifact.getFile(), classFileName));
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
        return project.getArtifacts().stream()
                .filter(artifact -> SCOPES.contains(artifact.getScope()))
                .map(artifact -> new Artifact(
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        artifact.getFile(),
                        artifact.getClassifier(),
                        artifact.getType()
                ));
    }
}
