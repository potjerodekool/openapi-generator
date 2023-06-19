package io.github.potjerodekool.openapi.internal.generate.annotation.openapi.response;

import io.github.potjerodekool.codegen.model.tree.expression.Expression;
import io.github.potjerodekool.openapi.internal.generate.annotation.AbstractAnnotationBuilder;

import java.util.List;

public class ApiResponsesAnnotationBuilder extends AbstractAnnotationBuilder<ApiResponsesAnnotationBuilder> {

    public ApiResponsesAnnotationBuilder() {
        super("io.swagger.v3.oas.annotations.responses.ApiResponses");
    }

    public ApiResponsesAnnotationBuilder value(final List<? extends Expression> value) {
        return addCompoundArray("value", value);
    }
}
