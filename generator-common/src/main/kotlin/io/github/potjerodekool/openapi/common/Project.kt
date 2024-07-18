package io.github.potjerodekool.openapi.common

import io.github.potjerodekool.openapi.common.dependency.DependencyChecker
import java.nio.file.Path

data class Project(
    val rootDir: Path,
    val sourceRoots: List<Path>,
    val resourcePaths: List<Path>,
    val generatedSourcesDirectory: Path,
    val dependencyChecker: DependencyChecker
)
