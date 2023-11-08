package io.github.potjerodekool.openapi.internal.generate.annotation.openapi.security;

import io.github.potjerodekool.codegen.model.tree.expression.LiteralExpression;
import io.github.potjerodekool.openapi.internal.generate.annotation.AbstractAnnotationBuilder;

import java.util.List;

public class SecurityRequirementAnnotationBuilder extends AbstractAnnotationBuilder<SecurityRequirementAnnotationBuilder> {

    public SecurityRequirementAnnotationBuilder() {
        super("io.swagger.v3.oas.annotations.security.SecurityRequirement");
    }

    public SecurityRequirementAnnotationBuilder name(final String name) {
        return add("name", name);
    }

    public SecurityRequirementAnnotationBuilder scopes(final List<String> scopes) {
        return addCompoundArray("scopes",
                scopes.stream()
                        .map(LiteralExpression::createStringLiteralExpression)
                        .toList()
        );
    }
}
