package io.github.potjerodekool.openapi.dependency;

import java.util.stream.Stream;

/**
 * A dependency checker to check is a dependency is available.
 */
public interface DependencyChecker {

    /**
     * @param groupId GroupId of the dependency.
     * @param artifactId ArtifactId of the dependency.
     * @return Returns true if the dependency is available during runtime.
     */
    boolean isDependencyPresent(String groupId,
                                String artifactId);

    boolean isClassPresent(String className);

    Stream<Artifact> getProjectArtifacts();
}
