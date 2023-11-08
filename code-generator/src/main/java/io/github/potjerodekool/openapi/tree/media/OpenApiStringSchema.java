package io.github.potjerodekool.openapi.tree.media;

import io.github.potjerodekool.openapi.internal.OpenApiSchemaBuilder;

public class OpenApiStringSchema extends OpenApiSchema<String> {
    public OpenApiStringSchema(final OpenApiSchemaBuilder schemaBuilder) {
        super(schemaBuilder);
    }

    @Override
    public String name() {
        return "name";
    }
}
