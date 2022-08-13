package io.github.potjerodekool.openapi.maven;

import io.github.potjerodekool.openapi.Artifact;
import io.github.potjerodekool.openapi.DependencyChecker;
import org.apache.maven.project.MavenProject;
import java.util.Set;
import java.util.stream.Stream;

public class MavenDependencyChecker implements DependencyChecker {

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
    public Stream<Artifact> getProjectArtifacts() {
        return project.getArtifacts().stream()
                .filter(artifact -> SCOPES.contains(artifact.getScope()))
                .map(artifact -> new Artifact(
                        artifact.getGroupId(),
                        artifact.getArtifactId(),
                        artifact.getFile()
                ));
    }
}
