package io.github.potjerodekool.openapi.common.generate.annotation.openapi.response;

import io.github.potjerodekool.codegen.template.model.expression.Expr;
import io.github.potjerodekool.openapi.common.generate.annotation.AbstractAnnotationBuilder;

import java.util.List;

public class ApiResponsesAnnotationBuilder extends AbstractAnnotationBuilder<ApiResponsesAnnotationBuilder> {

    public ApiResponsesAnnotationBuilder() {
        super("io.swagger.v3.oas.annotations.responses.ApiResponses");
    }

    public ApiResponsesAnnotationBuilder value(final List<? extends Expr> value) {
        return addCompoundArray("value", value);
    }
}
