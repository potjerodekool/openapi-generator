package io.github.potjerodekool.openapi.type;

import io.github.potjerodekool.openapi.tree.OpenApiProperty;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;

public interface OpenApiType {

    String name();

    default String qualifiedName() {
        return name();
    }

    default @Nullable String format() {
        return null;
    }

    default @Nullable Boolean nullable() { return null; }

    default Map<String, OpenApiProperty> properties() {
        return Map.of();
    }

    default @Nullable OpenApiProperty additionalProperties() {
        return null;
    }

    OpenApiTypeKind kind();
}
