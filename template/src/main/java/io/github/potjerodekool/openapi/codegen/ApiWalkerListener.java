package io.github.potjerodekool.openapi.codegen;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;

public interface ApiWalkerListener {
    default void visitOperation(OpenAPI openAPI, String path, Operation operation) {
    }

    default void visitSchema(OpenAPI openAPI, HttpMethod httpMethod, Schema<?> schema) {
    }

}
