package io.github.potjerodekool.openapi.internal.generate.annotation.openapi.media;

import io.github.potjerodekool.codegen.model.tree.expression.ArrayInitializerExpression;
import io.github.potjerodekool.codegen.model.tree.expression.Expression;
import io.github.potjerodekool.openapi.internal.generate.annotation.AbstractAnnotationBuilder;

import java.util.List;

public class ContentAnnotationBuilder extends AbstractAnnotationBuilder<ContentAnnotationBuilder> {

    public ContentAnnotationBuilder() {
        super("io.swagger.v3.oas.annotations.media.Content");
    }

    public ContentAnnotationBuilder schema(final Expression schema) {
        return add("schema", schema);
    }

    public ContentAnnotationBuilder mediaType(final String mediaType) {
        return add("mediaType", mediaType);
    }

    public ContentAnnotationBuilder examples(final List<Expression> examples) {
        return addCompoundArray("examples", examples);
    }

    public ContentAnnotationBuilder array(final Expression array) {
        if (array instanceof ArrayInitializerExpression) {
            throw new IllegalArgumentException();
        }

        return add("array", array);
    }

    public ContentAnnotationBuilder schemaProperties(final Expression... schemaProperties) {
        return addAttributeArray("schemaProperties", new ArrayInitializerExpression(List.of(schemaProperties)));
    }

    public ContentAnnotationBuilder additionalPropertiesSchema(final Expression additionalPropertiesSchema) {
        return add("additionalPropertiesSchema", additionalPropertiesSchema);
    }

}
