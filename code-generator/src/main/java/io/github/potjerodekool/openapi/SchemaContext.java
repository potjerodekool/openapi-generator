package io.github.potjerodekool.openapi;

import org.checkerframework.checker.nullness.qual.Nullable;

public record SchemaContext(HttpMethod httpMethod,
                            @Nullable RequestCycleLocation requestCycleLocation,
                            @Nullable ParameterLocation parameterLocation) {

    public SchemaContext(HttpMethod httpMethod,
                         RequestCycleLocation requestCycleLocation) {
        this(httpMethod, requestCycleLocation, null);
    }

    public SchemaContext(HttpMethod httpMethod,
                         ParameterLocation parameterLocation) {
        this(httpMethod, null, parameterLocation);
    }
}
