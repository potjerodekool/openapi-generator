package io.github.potjerodekool.openapi.internal.generate.annotation.spring.web;

import io.github.potjerodekool.openapi.internal.generate.annotation.AbstractAnnotationBuilder;

public class PathVariableAnnotationBuilder extends AbstractAnnotationBuilder<PathVariableAnnotationBuilder> {

    public PathVariableAnnotationBuilder() {
        super("org.springframework.web.bind.annotation.PathVariable");
    }

    public PathVariableAnnotationBuilder name(final String name) {
        return add("name", name);
    }

    public PathVariableAnnotationBuilder required(final Boolean required) {
        return add("required", required);
    }
}
