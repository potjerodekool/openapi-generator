package io.github.potjerodekool.openapi.internal.generate.annotation.openapi.security;

import io.github.potjerodekool.openapi.internal.generate.annotation.AbstractAnnotationBuilder;

public class SecurityRequirementAnnotationBuilder extends AbstractAnnotationBuilder<SecurityRequirementAnnotationBuilder> {

    public SecurityRequirementAnnotationBuilder() {
        super("io.swagger.v3.oas.annotations.security.SecurityRequirement");
    }

    public SecurityRequirementAnnotationBuilder name(final String name) {
        return add("name", name);
    }
}
