package io.github.potjerodekool.openapi.tree;

import io.github.potjerodekool.openapi.type.OpenApiType;
import org.checkerframework.checker.nullness.qual.Nullable;

public record OpenApiProperty(OpenApiType type,
                              boolean required,
                              @Nullable Boolean readOnly,
                              @Nullable Boolean writeOnly,
                              Constraints constraints) {

    public OpenApiProperty(OpenApiType type,
                           boolean required,
                           @Nullable Boolean readOnly,
                           @Nullable Boolean writeOnly) {
        this(
                type,
                required,
                readOnly,
                writeOnly,
                new Constraints()
        );
    }
}