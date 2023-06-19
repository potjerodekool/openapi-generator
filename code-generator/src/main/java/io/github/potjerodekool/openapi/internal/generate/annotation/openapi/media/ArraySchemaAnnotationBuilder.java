package io.github.potjerodekool.openapi.internal.generate.annotation.openapi.media;

import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.openapi.internal.generate.annotation.AbstractAnnotationBuilder;

public class ArraySchemaAnnotationBuilder extends AbstractAnnotationBuilder<ArraySchemaAnnotationBuilder> {

    public ArraySchemaAnnotationBuilder() {
        super("io.swagger.v3.oas.annotations.media.ArraySchema");
    }

    public ArraySchemaAnnotationBuilder schema(final AnnotationExpression schema) {
        return add("schema",schema);
    }
}
