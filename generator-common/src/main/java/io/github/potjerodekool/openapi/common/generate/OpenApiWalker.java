package io.github.potjerodekool.openapi.common.generate;

import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.Schema;

public final class OpenApiWalker {

    private OpenApiWalker() {
    }

    public static void walk(final OpenAPI api,
                            final OpenApiWalkerListener listener) {
        api.getPaths()
                .forEach((path, pathItem) -> visitPath(api, path, pathItem, listener));
    }

    private static void visitPath(final OpenAPI openAPI,
                                  final String path,
                                  final PathItem pathItem,
                                  final OpenApiWalkerListener listener) {
        visitOperation(openAPI, path, HttpMethod.POST, pathItem.getPost(), listener);
        visitOperation(openAPI, path, HttpMethod.GET, pathItem.getGet(), listener);
        visitOperation(openAPI, path, HttpMethod.PUT, pathItem.getPut(), listener);
        visitOperation(openAPI, path, HttpMethod.DELETE, pathItem.getDelete(), listener);
        visitOperation(openAPI, path, HttpMethod.PATCH, pathItem.getPatch(), listener);
        visitOperation(openAPI, path, HttpMethod.OPTIONS, pathItem.getOptions(), listener);
        visitOperation(openAPI, path, HttpMethod.HEAD, pathItem.getHead(), listener);
    }

    private static void visitOperation(final OpenAPI openAPI,
                                       final String path,
                                       final HttpMethod httpMethod,
                                       final Operation operation,
                                       final OpenApiWalkerListener listener) {
        if (operation != null) {
            listener.visitOperation(openAPI, httpMethod, path, operation);
            final var requestBody = operation.getRequestBody();

            if (requestBody != null) {
                final var content = requestBody.getContent();
                visitContent(openAPI, httpMethod, path, operation, content, listener);
            }

            final var responses = operation.getResponses();

            if (responses != null) {
                responses.forEach((responseCode, response) ->
                        visitContent(openAPI, httpMethod, path, operation, response.getContent(), listener));
            }
        }
    }

    private static void visitContent(final OpenAPI openAPI,
                                     final HttpMethod httpMethod,
                                     final String path,
                                     final Operation operation,
                                     final Content content,
                                     final OpenApiWalkerListener listener) {
        if (content != null) {
            final var mediaType = content.get(ContentTypes.JSON);

            if (mediaType != null) {
                final var schema = mediaType.getSchema();

                if (schema != null) {
                    visitSchema(openAPI, httpMethod, path, operation, schema, listener);
                }
            }
        }
    }

    private static void visitSchema(final OpenAPI openAPI,
                                    final HttpMethod httpMethod,
                                    final String path,
                                    final Operation operation,
                                    final Schema<?> schema,
                                    final OpenApiWalkerListener listener) {
        listener.visitSchema(openAPI, httpMethod, path, operation, schema, null);
    }

}
