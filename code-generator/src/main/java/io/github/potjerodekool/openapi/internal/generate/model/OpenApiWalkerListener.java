package io.github.potjerodekool.openapi.internal.generate.model;

import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;

public interface OpenApiWalkerListener {

    default void visitOperation(final OpenAPI api, HttpMethod method, String path, Operation operation) {
    }

}
