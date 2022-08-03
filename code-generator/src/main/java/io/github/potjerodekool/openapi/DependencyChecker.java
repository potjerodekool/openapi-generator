package io.github.potjerodekool.openapi;

public interface DependencyChecker {

    boolean isDependencyPresent(String groupId,
                                String artifactId);
}
