package io.github.potjerodekool.openapi.common.dependency;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;

public record Artifact(String groupId,
                       String artifactId,
                       @Nullable File file,
                       String classifier,
                       String type) {

}
