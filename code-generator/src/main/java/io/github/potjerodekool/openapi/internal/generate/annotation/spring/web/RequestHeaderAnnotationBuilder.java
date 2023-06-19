package io.github.potjerodekool.openapi.internal.generate.annotation.spring.web;

import io.github.potjerodekool.openapi.internal.generate.annotation.AbstractAnnotationBuilder;

public class RequestHeaderAnnotationBuilder extends AbstractAnnotationBuilder<RequestHeaderAnnotationBuilder> {

    public RequestHeaderAnnotationBuilder() {
        super("org.springframework.web.bind.annotation.RequestHeader");
    }

    public RequestHeaderAnnotationBuilder name(final String name) {
        return add("name", name);
    }

    public RequestHeaderAnnotationBuilder required(final Boolean required) {
        if (required != null && !required) {
            return add("required", false);
        } else {
            return this;
        }
    }
}
