package io.github.potjerodekool.openapi;

import io.github.potjerodekool.openapi.tree.OpenApiProperty;
import io.github.potjerodekool.openapi.type.OpenApiObjectType;
import io.github.potjerodekool.openapi.type.OpenApiStandardType;
import io.github.potjerodekool.openapi.util.Utils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;

public class ObjectBuilder {

    private @Nullable String name;
    private final Map<String, OpenApiProperty> properties = new HashMap<>();

    public ObjectBuilder name(final String name) {
        this.name = name;
        return this;
    }

    public ObjectBuilder property(final String name,
                                  final OpenApiStandardType type,
                                  final Boolean required,
                                  final @Nullable Boolean readOnly,
                                  final @Nullable Boolean writeOnly) {
        properties.put(name, new OpenApiProperty(type, required, readOnly, writeOnly));
        return this;
    }

    public OpenApiObjectType build() {
        return new OpenApiObjectType(Utils.requireNonNull(name), properties, null);
    }
}
