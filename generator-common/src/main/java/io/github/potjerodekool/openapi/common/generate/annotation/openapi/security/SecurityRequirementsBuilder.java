package io.github.potjerodekool.openapi.common.generate.annotation.openapi.security;

import io.github.potjerodekool.codegen.template.model.expression.Expr;
import io.github.potjerodekool.openapi.common.generate.annotation.AbstractAnnotationBuilder;

import java.util.List;

public class SecurityRequirementsBuilder extends AbstractAnnotationBuilder<SecurityRequirementsBuilder> {

    public SecurityRequirementsBuilder() {
        super("io.swagger.v3.oas.annotations.security.SecurityRequirements");
    }

    public <E extends Expr> SecurityRequirementsBuilder value(final List<E> array) {
        return addAttributeArray("value", array);
    }
}
