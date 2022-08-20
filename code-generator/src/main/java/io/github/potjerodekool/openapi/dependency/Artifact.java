package io.github.potjerodekool.openapi.dependency;

import java.io.File;

public record Artifact(String groupId, String artifactId, File file) {

}
