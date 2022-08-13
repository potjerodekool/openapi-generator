package io.github.potjerodekool.openapi.internal;

import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.ParameterLocation;
import io.github.potjerodekool.openapi.RequestCycleLocation;
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
