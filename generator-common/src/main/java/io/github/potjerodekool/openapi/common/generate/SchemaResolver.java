package io.github.potjerodekool.openapi.common.generate;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

public final class SchemaResolver {

    private static final String COMPONENT_SCHEMA_PREFIX = "#/components/schemas/";

    private SchemaResolver() {
    }

    public static ResolvedSchemaResult resolve(final OpenAPI openAPI,
                                               final Schema<?> schema) {
        final var components = openAPI.getComponents();

        if (components == null) {
            return new ResolvedSchemaResult(schema.getName(), schema);
        }

        final var schemas = components.getSchemas();

        if (schemas == null) {
            return new ResolvedSchemaResult(schema.getName(), schema);
        }

        final var ref = schema.get$ref();

        if (ref != null && ref.startsWith(COMPONENT_SCHEMA_PREFIX)) {
            final var name = ref.substring(COMPONENT_SCHEMA_PREFIX.length());
            final var resolvedSchema = schemas.get(name);
            return new ResolvedSchemaResult(name, resolvedSchema);
        }

        return new ResolvedSchemaResult(schema.getName(), schema);
    }
}
