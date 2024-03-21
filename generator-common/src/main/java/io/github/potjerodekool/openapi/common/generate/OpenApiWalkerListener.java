package io.github.potjerodekool.openapi.common.generate;

import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;

public interface OpenApiWalkerListener {

    default void visitOperation(OpenAPI api,
                                HttpMethod method,
                                String path,
                                Operation operation) {
    }

    default void visitContent(OpenAPI openAPI,
                              HttpMethod httpMethod,
                              String path,
                              Operation operation, final Content content) {
    }

    default void visitSchema(OpenAPI openAPI,
                             HttpMethod httpMethod,
                             String path,
                             Operation operation,
                             Schema<?> schema,
                             String schemaName) {
    }

}
