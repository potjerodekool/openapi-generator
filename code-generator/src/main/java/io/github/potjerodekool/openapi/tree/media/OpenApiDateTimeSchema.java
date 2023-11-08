package io.github.potjerodekool.openapi.tree.media;

import io.github.potjerodekool.openapi.internal.OpenApiSchemaBuilder;

import java.time.OffsetDateTime;

public class OpenApiDateTimeSchema extends OpenApiSchema<OffsetDateTime> {
    public OpenApiDateTimeSchema(final OpenApiSchemaBuilder schemaBuilder) {
        super(schemaBuilder);
    }
}
