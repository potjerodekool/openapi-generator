package io.github.potjerodekool.openapi.tree;

import org.checkerframework.checker.nullness.qual.Nullable;

public record OpenApiPath(String path,
                          @Nullable OpenApiOperation post,
                          @Nullable OpenApiOperation get,
                          @Nullable OpenApiOperation put,
                          @Nullable OpenApiOperation patch,
                          @Nullable OpenApiOperation delete,
                          String creatingReference) {

}
