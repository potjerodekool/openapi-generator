package io.github.potjerodekool.openapi.tree;

import io.github.potjerodekool.openapi.tree.media.OpenApiSchema;

import java.util.Map;

public record OpenApiContent(String ref,
                             OpenApiSchema<?> schema,
                             Map<String, OpenApiExample> examples) {

}
