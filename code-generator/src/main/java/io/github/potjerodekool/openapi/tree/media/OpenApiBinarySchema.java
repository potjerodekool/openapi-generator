package io.github.potjerodekool.openapi.tree.media;

import io.github.potjerodekool.openapi.internal.OpenApiSchemaBuilder;

public class OpenApiBinarySchema extends OpenApiSchema<byte[]> {
    public OpenApiBinarySchema(final OpenApiSchemaBuilder schemaBuilder) {
        super(schemaBuilder);
    }
}
