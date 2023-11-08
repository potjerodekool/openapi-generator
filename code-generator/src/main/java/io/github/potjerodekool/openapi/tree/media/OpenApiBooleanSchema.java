package io.github.potjerodekool.openapi.tree.media;

import io.github.potjerodekool.openapi.internal.OpenApiSchemaBuilder;

public class OpenApiBooleanSchema extends OpenApiSchema<Boolean> {
    public OpenApiBooleanSchema(final OpenApiSchemaBuilder schemaBuilder) {
        super(schemaBuilder);
    }

    @Override
    public String name() {
        return "boolean";
    }
}
