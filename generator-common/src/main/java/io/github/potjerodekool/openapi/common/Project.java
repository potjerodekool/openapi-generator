package io.github.potjerodekool.openapi.common;

import io.github.potjerodekool.openapi.common.dependency.DependencyChecker;

import java.nio.file.Path;
import java.util.List;

public record Project(Path rootDir,
                      List<Path> sourceRoots,
                      List<Path> resourcePaths,
                      Path generatedSourcesDirectory,
                      DependencyChecker dependencyChecker) {

}
