package io.github.potjerodekool.openapi.tree;

import io.github.potjerodekool.openapi.tree.enums.OpenApiSecuritySchemeIn;
import io.github.potjerodekool.openapi.tree.enums.OpenApiSecuritySchemeType;
import org.checkerframework.checker.nullness.qual.Nullable;

public record OpenApiSecurityScheme(OpenApiSecuritySchemeType type,
                                    String name,
                                    String description,
                                    String paramName,
                                    @Nullable OpenApiSecuritySchemeIn in,
                                    String schema,
                                    String bearerFormat) {

}
