package io.github.potjerodekool.openapi.tree;

import io.github.potjerodekool.openapi.type.OpenApiType;

import java.util.Map;

public record OpenApiContent(OpenApiType schema,
                             Map<String, OpenApiExample> examples) {
}
