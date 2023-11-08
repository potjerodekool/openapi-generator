package io.github.potjerodekool.openapi.tree.media;

import io.github.potjerodekool.openapi.internal.OpenApiSchemaBuilder;

import java.util.UUID;

public class OpenApiUUIDSchema extends OpenApiSchema<UUID> {
    public OpenApiUUIDSchema(final OpenApiSchemaBuilder schemaBuilder) {
        super(schemaBuilder);
    }
}
