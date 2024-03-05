package io.github.potjerodekool.openapi.common.generate.annotation.openapi.media;

import io.github.potjerodekool.codegen.model.tree.expression.ArrayInitializerExpression;
import io.github.potjerodekool.codegen.template.model.expression.ArrayExpr;
import io.github.potjerodekool.codegen.template.model.expression.Expr;
import io.github.potjerodekool.openapi.common.generate.annotation.AbstractAnnotationBuilder;

import java.util.List;

public class ContentAnnotationBuilder extends AbstractAnnotationBuilder<ContentAnnotationBuilder> {

    public ContentAnnotationBuilder() {
        super("io.swagger.v3.oas.annotations.media.Content");
    }

    public ContentAnnotationBuilder schema(final Expr schema) {
        return add("schema", schema);
    }

    public ContentAnnotationBuilder mediaType(final String mediaType) {
        return add("mediaType", mediaType);
    }

    public <E extends Expr> ContentAnnotationBuilder examples(final List<Expr> examples) {
        return addCompoundArray("examples", examples);
    }

    public ContentAnnotationBuilder array(final Expr array) {
        if (array instanceof ArrayInitializerExpression) {
            throw new IllegalArgumentException();
        }

        return add("array", array);
    }

    public <E extends Expr> ContentAnnotationBuilder schemaProperties(final E... schemaProperties) {
        return addAttributeArray("schemaProperties", new ArrayExpr().values(schemaProperties));
    }

    public ContentAnnotationBuilder additionalPropertiesSchema(final Expr additionalPropertiesSchema) {
        return add("additionalPropertiesSchema", additionalPropertiesSchema);
    }

}
