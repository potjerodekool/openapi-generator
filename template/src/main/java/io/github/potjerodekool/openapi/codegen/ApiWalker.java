package io.github.potjerodekool.openapi.codegen;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;

public class ApiWalker {
    public void walk(final OpenAPI openAPI,
                     final ApiWalkerListener listener) {
        openAPI.getPaths().forEach((path, pathItem) -> {
            visitoPath(openAPI, path, pathItem, listener);
        });
    }

    private void visitoPath(final OpenAPI openAPI,
                            final String path,
                            final PathItem pathItem,
                            final ApiWalkerListener listener) {
        visitOperation(openAPI, path, HttpMethod.POST, pathItem.getPost(), listener);
        visitOperation(openAPI, path, HttpMethod.GET, pathItem.getGet(), listener);
        visitOperation(openAPI, path, HttpMethod.PUT, pathItem.getPut(), listener);
        visitOperation(openAPI, path, HttpMethod.DELETE, pathItem.getDelete(), listener);
        visitOperation(openAPI, path, HttpMethod.PATCH, pathItem.getPatch(), listener);
        visitOperation(openAPI, path, HttpMethod.OPTIONS, pathItem.getOptions(), listener);
        visitOperation(openAPI, path, HttpMethod.HEAD, pathItem.getHead(), listener);
        visitOperation(openAPI, path, HttpMethod.TRACE, pathItem.getTrace(), listener);
    }

    private void visitOperation(final OpenAPI openAPI,
                                final String path,
                                final HttpMethod httpMethod,
                                final Operation operation,
                                final ApiWalkerListener listener) {
        if (operation != null) {
            listener.visitOperation(openAPI, path, operation);
            final var requestBody = operation.getRequestBody();

            if (requestBody != null) {
                final var content = requestBody.getContent();
                visitContent(openAPI, httpMethod, content, listener);
            }

            final var responses = operation.getResponses();

            if (responses != null) {
                responses.forEach((responseCode, response) -> {
                    visitContent(openAPI, httpMethod, response.getContent(), listener);
                });
            }
        }
    }

    private void visitContent(final OpenAPI openAPI,
                              final HttpMethod httpMethod,
                              final Content content,
                              final ApiWalkerListener listener) {
        if (content != null) {
            final var mediaType = content.get(MediaTypes.JSON);

            if (mediaType != null) {
                final var schema = mediaType.getSchema();

                if (schema != null) {
                    visitSchema(openAPI, httpMethod, schema, listener);
                }
            }
        }
    }

    private void visitSchema(final OpenAPI openAPI,
                             final HttpMethod httpMethod,
                             final Schema<?> schema,
                             final ApiWalkerListener listener) {
        listener.visitSchema(openAPI, httpMethod, schema);
    }
}
