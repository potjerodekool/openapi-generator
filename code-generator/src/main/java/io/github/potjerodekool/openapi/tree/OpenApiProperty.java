package io.github.potjerodekool.openapi.tree;

import io.github.potjerodekool.openapi.type.OpenApiType;

public record OpenApiProperty(OpenApiType type,
                              boolean required,
                              boolean readOnly) {
}
