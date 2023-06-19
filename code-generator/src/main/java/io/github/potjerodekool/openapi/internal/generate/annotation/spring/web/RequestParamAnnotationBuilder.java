package io.github.potjerodekool.openapi.internal.generate.annotation.spring.web;

import io.github.potjerodekool.openapi.internal.generate.annotation.AbstractAnnotationBuilder;

public class RequestParamAnnotationBuilder extends AbstractAnnotationBuilder<RequestParamAnnotationBuilder> {

    public RequestParamAnnotationBuilder() {
        super("org.springframework.web.bind.annotation.RequestParam");
    }

    public RequestParamAnnotationBuilder name(final String name) {
        return add("name", name);
    }

    public RequestParamAnnotationBuilder required(final Boolean required) {
        return add("required", required);
    }
}
