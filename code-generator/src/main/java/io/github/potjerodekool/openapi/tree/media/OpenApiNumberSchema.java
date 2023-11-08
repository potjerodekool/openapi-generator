package io.github.potjerodekool.openapi.tree.media;

import io.github.potjerodekool.openapi.internal.OpenApiSchemaBuilder;

import java.math.BigDecimal;

public class OpenApiNumberSchema extends OpenApiSchema<BigDecimal> {
    public OpenApiNumberSchema(final OpenApiSchemaBuilder schemaBuilder) {
        super(schemaBuilder);
    }

    @Override
    public String name() {
        return "number";
    }
}
