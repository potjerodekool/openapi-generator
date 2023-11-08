package io.github.potjerodekool.openapi.tree.media;

import io.github.potjerodekool.openapi.internal.OpenApiSchemaBuilder;

public class OpenApiArraySchema extends OpenApiSchema<Object> {

    public OpenApiArraySchema(final OpenApiSchemaBuilder schemaBuilder) {
        super(schemaBuilder);
    }

    @Override
    public String name() {
        return "array";
    }
}
