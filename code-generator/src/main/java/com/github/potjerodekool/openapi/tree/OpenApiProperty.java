package com.github.potjerodekool.openapi.tree;

import com.github.potjerodekool.openapi.type.OpenApiType;

public record OpenApiProperty(OpenApiType type,
                              boolean required,
                              boolean nullable,
                              boolean readOnly) {
}
