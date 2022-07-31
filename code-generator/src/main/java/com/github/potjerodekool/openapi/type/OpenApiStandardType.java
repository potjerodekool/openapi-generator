package com.github.potjerodekool.openapi.type;

import com.github.potjerodekool.openapi.tree.OpenApiProperty;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Set;

public class OpenApiStandardType implements OpenApiType {

    private static final Set<String> TYPES = Set.of(
            "string",
            "integer",
            "date",
            "date-time"
    );

    private final String type;
    private final @Nullable String format;

    public OpenApiStandardType(final String type,
                               final @Nullable String format) {
        this.type = type;
        this.format = format;
    }

    @Override
    public @Nullable String name() {
        return type;
    }

    @Override
    public @Nullable String format() {
        return format;
    }

    @Override
    public Map<String, OpenApiProperty> properties() {
        return Map.of();
    }

    @Override
    public @Nullable OpenApiProperty additionalProperties() {
        return null;
    }

    public String type() {
        return type;
    }

    public static boolean isStandardType(final String type) {
        return TYPES.contains(type);
    }
}
