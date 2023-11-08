package io.github.potjerodekool.openapi.internal.generate.annotation.spring.web;

import io.github.potjerodekool.openapi.internal.generate.annotation.AbstractAnnotationBuilder;

public class RequestBodyAnnotationBuilder extends AbstractAnnotationBuilder<RequestBodyAnnotationBuilder> {

    public RequestBodyAnnotationBuilder() {
        super("org.springframework.web.bind.annotation.RequestBody");
    }

    public RequestBodyAnnotationBuilder required(final Boolean required) {
        if (required != null) {
            return add("required", required);
        } else {
            return this;
        }
    }
}
