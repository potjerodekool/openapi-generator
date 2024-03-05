package io.github.potjerodekool.openapi.common.generate.annotation.openapi;

import io.github.potjerodekool.codegen.template.model.annotation.Annot;
import io.github.potjerodekool.codegen.template.model.expression.Expr;
import io.github.potjerodekool.openapi.common.generate.annotation.AbstractAnnotationBuilder;

public class ParameterAnnotationBuilder extends AbstractAnnotationBuilder<ParameterAnnotationBuilder> {

    public ParameterAnnotationBuilder() {
        super("io.swagger.v3.oas.annotations.Parameter");
    }

    public ParameterAnnotationBuilder name(final String name) {
        return add("name", name);
    }

    public ParameterAnnotationBuilder in(final Expr in) {
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

    public ParameterAnnotationBuilder explode(final Expr explode) {
        return add("explode", explode);
    }

    public ParameterAnnotationBuilder example(final String example) {
        return add("example", example);
    }

    public ParameterAnnotationBuilder schema(final Annot schemaAnnotation) {
        return add("schema", schemaAnnotation);
    }
}
