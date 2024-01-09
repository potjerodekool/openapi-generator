package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.openapi.internal.generate.ContentTypes;
import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.util.Objects;

public abstract class AbstractOpenApiWalkerListener implements OpenApiWalkerListener {
    @Override
    public void visitOperation(final OpenAPI api, final HttpMethod method, final String path, final Operation operation) {
        final var requestBody = operation.getRequestBody();

        if (requestBody != null) {
            visitContent(api, method, path, operation, requestBody.getContent());
        }

        final var responses = operation.getResponses();

        if (responses != null) {
            responses.keySet().stream()
                    .map(responses::get)
                    .filter(Objects::nonNull)
                    .map(ApiResponse::getContent)
                    .filter(Objects::nonNull)
                    .forEach(content -> visitContent(api, method, path, operation, content));
        }
    }

    protected void visitContent(final OpenAPI api,
                                final HttpMethod method,
                                final String path,
                                final Operation operation,
                                final Content content) {
        if (content == null) {
            return;
        }

        final var jsonContent = content.get(ContentTypes.JSON);

        if (jsonContent == null) {
            return;
        }

        final var schema = jsonContent.getSchema();

        if (schema == null) {
            return;
        }

        visitSchema(api, method, path, operation, schema);
    }

    protected void visitSchema(final OpenAPI api,
                               final HttpMethod method,
                               final String path,
                               final Operation operation,
                               final Schema<?> schema) {

    }
}
