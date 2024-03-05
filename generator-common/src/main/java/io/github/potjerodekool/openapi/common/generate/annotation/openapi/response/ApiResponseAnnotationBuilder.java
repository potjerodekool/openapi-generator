package io.github.potjerodekool.openapi.common.generate.annotation.openapi.response;

import io.github.potjerodekool.codegen.template.model.expression.ArrayExpr;
import io.github.potjerodekool.codegen.template.model.expression.Expr;
import io.github.potjerodekool.openapi.common.generate.annotation.AbstractAnnotationBuilder;

import java.util.List;

public class ApiResponseAnnotationBuilder extends AbstractAnnotationBuilder<ApiResponseAnnotationBuilder> {

    public ApiResponseAnnotationBuilder() {
        super("io.swagger.v3.oas.annotations.responses.ApiResponse");
    }

    public <E extends Expr> ApiResponseAnnotationBuilder content(final List<E> content) {
        return addAttributeArray("content", content);
    }

    public ApiResponseAnnotationBuilder headers(final ArrayExpr headers) {
        return addAttributeArray("headers", headers);
    }

    public ApiResponseAnnotationBuilder description(final String description) {
        return add("description", description);
    }

    public ApiResponseAnnotationBuilder responseCode(final String responseCode) {
        if ("default".equals(responseCode)) {
            return this;
        }
        return add("responseCode", responseCode);
    }

}
