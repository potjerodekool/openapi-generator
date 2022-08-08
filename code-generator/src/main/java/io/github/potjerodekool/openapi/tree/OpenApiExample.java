package io.github.potjerodekool.openapi.tree;

import org.checkerframework.checker.nullness.qual.Nullable;

public record OpenApiExample(@Nullable String summary,
                             @Nullable String description,
                             Object value) {
}
