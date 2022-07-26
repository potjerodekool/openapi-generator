package com.github.potjerodekool.openapi.type;

import com.github.potjerodekool.openapi.tree.OpenApiProperty;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;

public final class OpenApiArrayType implements OpenApiType {

    private final OpenApiType items;

    public OpenApiArrayType(final OpenApiType items) {
        this.items = items;
    }

    @Override
    public @Nullable String name() {
        return null;
    }

    @Override
    public @Nullable String format() {
        return null;
    }

    @Override
    public Map<String, OpenApiProperty> properties() {
        return Map.of();
    }

    @Override
    public @Nullable OpenApiProperty additionalProperties() {
        return null;
    }

    public OpenApiType getItems() {
        return items;
    }
}
