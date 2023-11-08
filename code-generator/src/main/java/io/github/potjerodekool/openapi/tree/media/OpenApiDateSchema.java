package io.github.potjerodekool.openapi.tree.media;

import io.github.potjerodekool.openapi.internal.OpenApiSchemaBuilder;

import java.util.Date;

public class OpenApiDateSchema extends OpenApiSchema<Date> {
    public OpenApiDateSchema(final OpenApiSchemaBuilder schemaBuilder) {
        super(schemaBuilder);
    }
}
