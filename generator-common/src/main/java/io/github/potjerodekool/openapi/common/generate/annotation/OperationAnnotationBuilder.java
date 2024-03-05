package io.github.potjerodekool.openapi.common.generate.annotation;

import io.github.potjerodekool.codegen.template.model.annotation.Annot;

import java.util.List;

public class OperationAnnotationBuilder extends AbstractAnnotationBuilder<OperationAnnotationBuilder> {

    public OperationAnnotationBuilder() {
        super("io.swagger.v3.oas.annotations.Operation");
    }

    public OperationAnnotationBuilder summary(final String summary) {
        return add("summary", summary);
    }

    public OperationAnnotationBuilder operationId(final String operationId) {
        return add("operationId", operationId);
    }

    public OperationAnnotationBuilder tags(final List<String> tags) {
        return addStringArray("tags", tags);
    }

    public OperationAnnotationBuilder requestBody(final Annot requestBodyAnnotation) {
        return add("requestBody", requestBodyAnnotation);
    }
}
