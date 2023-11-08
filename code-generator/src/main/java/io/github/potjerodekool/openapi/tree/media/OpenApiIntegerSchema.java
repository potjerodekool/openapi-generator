package io.github.potjerodekool.openapi.tree.media;

import io.github.potjerodekool.openapi.internal.OpenApiSchemaBuilder;

public class OpenApiIntegerSchema extends OpenApiSchema<Number> {
    public OpenApiIntegerSchema(final OpenApiSchemaBuilder schemaBuilder) {
        super(schemaBuilder);
    }

    @Override
    public String name() {
        return "integer";
    }
}
