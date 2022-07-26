package com.github.potjerodekool.openapi.type;

import com.github.potjerodekool.openapi.tree.OpenApiProperty;
import com.github.potjerodekool.openapi.tree.Package;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;

public record OpenApiObjectType(Package pck,
                                String name,
                                Map<String, OpenApiProperty> properties,
                                @Nullable OpenApiProperty additionalProperties) implements OpenApiType {

    public OpenApiObjectType(final String name,
                             final Map<String, OpenApiProperty> properties,
                             final @Nullable OpenApiProperty additionalProperties) {
        this(Package.UNNAMED, name, properties, additionalProperties);
    }

    @Override
    public @Nullable String format() {
        return null;
    }

    public OpenApiObjectType withPackage(final Package pck) {
        return new OpenApiObjectType(pck, name, properties, additionalProperties);
    }

    public OpenApiObjectType withName(final String name) {
        return new OpenApiObjectType(pck, name, properties, additionalProperties);
    }
}
