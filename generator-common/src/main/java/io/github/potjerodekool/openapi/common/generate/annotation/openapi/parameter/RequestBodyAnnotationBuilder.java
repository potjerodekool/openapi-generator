package io.github.potjerodekool.openapi.common.generate.annotation.openapi.parameter;

import io.github.potjerodekool.codegen.template.model.annotation.Annot;
import io.github.potjerodekool.openapi.common.generate.annotation.AbstractAnnotationBuilder;

import java.util.List;

public class RequestBodyAnnotationBuilder extends AbstractAnnotationBuilder<RequestBodyAnnotationBuilder> {
    public RequestBodyAnnotationBuilder() {
        super("io.swagger.v3.oas.annotations.parameters.RequestBody");
    }

    public RequestBodyAnnotationBuilder content(final List<Annot> contents) {
        return addCompoundArray("content", contents);
    }
}
