package io.github.potjerodekool.openapi;

import org.checkerframework.checker.nullness.qual.Nullable;

public record SchemaContext(HttpMethod httpMethod,
                            @Nullable RequestCycleLocation requestCycleLocation,
                            @Nullable ParameterLocation parameterLocation) {

    public SchemaContext(final HttpMethod httpMethod,
                         final RequestCycleLocation requestCycleLocation) {
        this(httpMethod, requestCycleLocation, null);
    }

    public SchemaContext(final HttpMethod httpMethod,
                         final ParameterLocation parameterLocation) {
        this(httpMethod, null, parameterLocation);
    }
}
