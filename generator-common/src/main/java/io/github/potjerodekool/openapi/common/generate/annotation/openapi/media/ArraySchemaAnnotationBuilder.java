package io.github.potjerodekool.openapi.common.generate.annotation.openapi.media;

import io.github.potjerodekool.codegen.template.model.annotation.Annot;
import io.github.potjerodekool.openapi.common.generate.annotation.AbstractAnnotationBuilder;

public class ArraySchemaAnnotationBuilder extends AbstractAnnotationBuilder<ArraySchemaAnnotationBuilder> {

    public ArraySchemaAnnotationBuilder() {
        super("io.swagger.v3.oas.annotations.media.ArraySchema");
    }

    public ArraySchemaAnnotationBuilder schema(final Annot schema) {
        return add("schema", schema);
    }
}
