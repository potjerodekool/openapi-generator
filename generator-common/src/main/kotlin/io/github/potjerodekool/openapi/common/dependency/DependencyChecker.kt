package io.github.potjerodekool.openapi.common.dependency

import java.util.stream.Stream

/**
 * A dependency checker to check is a dependency is available.
 */
interface DependencyChecker {
    /**
     * @param groupId GroupId of the dependency.
     * @param artifactId ArtifactId of the dependency.
     * @return Returns true if the dependency is available during runtime.
     */
    fun isDependencyPresent(
        groupId: String?,
        artifactId: String?
    ): Boolean

    fun isClassPresent(className: String?): Boolean

    val projectArtifacts: Stream<Artifact>
}
