package io.github.potjerodekool.openapi.internal.generate.model;

import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;

public final class OpenApiWalker {

    private OpenApiWalker() {
    }

    public static void walk(final OpenAPI api,
                            final OpenApiWalkerListener listener) {
        walkPaths(api, listener);
    }

    private static void walkPaths(final OpenAPI api,
                                  final OpenApiWalkerListener listener) {
        api.getPaths().forEach((path, pathItem) -> {
            walkOperation(api, HttpMethod.POST, path, pathItem.getPost(), listener);
            walkOperation(api, HttpMethod.GET, path, pathItem.getGet(), listener);
            walkOperation(api, HttpMethod.PUT, path, pathItem.getPut(), listener);
            walkOperation(api, HttpMethod.PATCH, path, pathItem.getPatch(), listener);
            walkOperation(api, HttpMethod.DELETE, path, pathItem.getDelete(), listener);
        });
    }

    private static void walkOperation(final OpenAPI api,
                                      final HttpMethod method,
                                      final String path,
                                      final Operation operation,
                                      final OpenApiWalkerListener listener) {
        if (operation != null) {
            listener.visitOperation(api, method, path, operation);
        }
    }

}
