package io.github.potjerodekool.openapi.internal.generate.annotation.openapi;

import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.expression.Expression;
import io.github.potjerodekool.openapi.internal.generate.annotation.AbstractAnnotationBuilder;

public class ParameterAnnotationBuilder extends AbstractAnnotationBuilder<ParameterAnnotationBuilder> {

    public ParameterAnnotationBuilder() {
        super("io.swagger.v3.oas.annotations.Parameter");
    }

    public ParameterAnnotationBuilder name(final String name) {
        return add("name", name);
    }

    public ParameterAnnotationBuilder in(final Expression in) {
        return add("in", in);
    }

    public ParameterAnnotationBuilder description(final String description) {
        return add("description", description);
    }

    public ParameterAnnotationBuilder required(final Boolean required) {
        return add("required", required);
    }

    public ParameterAnnotationBuilder allowEmptyValue(final Boolean allowEmptyValue) {
        return add("allowEmptyValue", allowEmptyValue);
    }

    public ParameterAnnotationBuilder explode(final Expression explode) {
        return add("explode", explode);
    }

    public ParameterAnnotationBuilder example(final String example) {
        return add("example", example);
    }

    public ParameterAnnotationBuilder schema(final AnnotationExpression schemaAnnotation) {
        return add("schema", schemaAnnotation);
    }
}
