package io.github.potjerodekool.openapi.tree;

import io.github.potjerodekool.openapi.tree.media.OpenApiSchema;

import java.util.Map;

public record OpenApiComponents(Map<String, OpenApiSchema<?>> schemas) {
}
