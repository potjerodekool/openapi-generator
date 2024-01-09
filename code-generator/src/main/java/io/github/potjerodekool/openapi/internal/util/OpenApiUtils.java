package io.github.potjerodekool.openapi.internal.util;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

import java.util.Map;

public final class OpenApiUtils {

    private static final String SCHEMAS_PREFIX = "#/components/schemas/";

    private OpenApiUtils() {
    }

    public static Schema<?> findComponentSchemaByName(final OpenAPI openAPI,
                                                      final String name) {
        final String schemaName;

        if (name.startsWith(SCHEMAS_PREFIX)) {
            schemaName = name.substring(SCHEMAS_PREFIX.length());
        } else {
            schemaName = name;
        }

        final var schemas = getComponentSchemas(openAPI);
        return schemas.get(schemaName);
    }

    public static String getSchemaName(final String ref) {
        if (ref.startsWith(SCHEMAS_PREFIX)) {
            return ref.substring(SCHEMAS_PREFIX.length());
        }
        return null;
    }

    public static Map<String, Schema> getComponentSchemas(final OpenAPI openAPI) {
        final var components = openAPI.getComponents();

        if (components == null) {
            return Map.of();
        }

        final var schemas = components.getSchemas();
        return schemas != null
                ? schemas
                : Map.of();
    }

}
