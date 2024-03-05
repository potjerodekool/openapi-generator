package io.github.potjerodekool.openapi.common.generate.annotation.openapi.media;

import io.github.potjerodekool.openapi.common.generate.annotation.AbstractAnnotationBuilder;

public class SchemaPropertyAnnotationBuilder extends AbstractAnnotationBuilder<SchemaPropertyAnnotationBuilder> {

    public SchemaPropertyAnnotationBuilder() {
        super("io.swagger.v3.oas.annotations.media.SchemaProperty");
    }

    public SchemaPropertyAnnotationBuilder name(final String name) {
        return add("name", name);
    }
}
