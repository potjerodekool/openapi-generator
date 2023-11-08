package io.github.potjerodekool.openapi.internal.generate.annotation.openapi.parameter;

import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.openapi.internal.generate.annotation.AbstractAnnotationBuilder;

import java.util.List;

public class RequestBodyAnnotationBuilder extends AbstractAnnotationBuilder<RequestBodyAnnotationBuilder> {
    public RequestBodyAnnotationBuilder() {
        super("io.swagger.v3.oas.annotations.parameters.RequestBody");
    }

    public RequestBodyAnnotationBuilder content(final List<AnnotationExpression> contents) {
        return addCompoundArray("content", contents);
    }
}
