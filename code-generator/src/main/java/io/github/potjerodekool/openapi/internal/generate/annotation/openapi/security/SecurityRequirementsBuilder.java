package io.github.potjerodekool.openapi.internal.generate.annotation.openapi.security;

import io.github.potjerodekool.codegen.model.tree.expression.Expression;
import io.github.potjerodekool.openapi.internal.generate.annotation.AbstractAnnotationBuilder;

import java.util.List;

public class SecurityRequirementsBuilder extends AbstractAnnotationBuilder<SecurityRequirementsBuilder> {

    public SecurityRequirementsBuilder() {
        super("io.swagger.v3.oas.annotations.security.SecurityRequirements");
    }

    public SecurityRequirementsBuilder value(final List<Expression> array) {
        return addAttributeArray("value", array);
    }
}
