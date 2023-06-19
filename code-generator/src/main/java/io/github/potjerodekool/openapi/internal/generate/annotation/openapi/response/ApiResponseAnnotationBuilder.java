package io.github.potjerodekool.openapi.internal.generate.annotation.openapi.response;

import io.github.potjerodekool.codegen.model.tree.expression.ArrayInitializerExpression;
import io.github.potjerodekool.codegen.model.tree.expression.Expression;
import io.github.potjerodekool.openapi.internal.generate.annotation.AbstractAnnotationBuilder;

import java.util.List;

public class ApiResponseAnnotationBuilder extends AbstractAnnotationBuilder<ApiResponseAnnotationBuilder> {

    public ApiResponseAnnotationBuilder() {
        super("io.swagger.v3.oas.annotations.responses.ApiResponse");
    }

    public ApiResponseAnnotationBuilder content(final List<Expression> content) {
        return addAttributeArray("content", content);
    }

    public ApiResponseAnnotationBuilder headers(final ArrayInitializerExpression headers) {
        return addAttributeArray("headers", headers);
    }

    public ApiResponseAnnotationBuilder description(final String description) {
        return add("description", description);
    }

    public ApiResponseAnnotationBuilder responseCode(final String responseCode) {
        return add("responseCode", responseCode);
    }

}
