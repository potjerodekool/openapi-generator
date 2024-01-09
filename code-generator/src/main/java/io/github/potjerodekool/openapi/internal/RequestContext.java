package io.github.potjerodekool.openapi.internal;

import io.github.potjerodekool.openapi.ParameterLocation;
import io.github.potjerodekool.openapi.RequestCycleLocation;
import io.swagger.models.HttpMethod;
import org.checkerframework.checker.nullness.qual.Nullable;

public record RequestContext(HttpMethod httpMethod,
                             @Nullable RequestCycleLocation requestCycleLocation,
                             @Nullable ParameterLocation parameterLocation) {

    public RequestContext(final HttpMethod httpMethod,
                          final RequestCycleLocation requestCycleLocation) {
        this(httpMethod, requestCycleLocation, null);
    }

    public RequestContext(final HttpMethod httpMethod,
                          final ParameterLocation parameterLocation) {
        this(httpMethod, null, parameterLocation);
    }
}
