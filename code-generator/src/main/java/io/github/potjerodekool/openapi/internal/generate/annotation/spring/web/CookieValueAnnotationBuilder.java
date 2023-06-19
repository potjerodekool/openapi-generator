package io.github.potjerodekool.openapi.internal.generate.annotation.spring.web;

import io.github.potjerodekool.openapi.internal.generate.annotation.AbstractAnnotationBuilder;

public class CookieValueAnnotationBuilder extends AbstractAnnotationBuilder<CookieValueAnnotationBuilder> {

    public CookieValueAnnotationBuilder() {
        super("org.springframework.web.bind.annotation.CookieValue");
    }

    public CookieValueAnnotationBuilder name(final String name) {
        return add("name", name);
    }

    public CookieValueAnnotationBuilder required(final Boolean required) {
        if (required != null && !required) {
            return add("required", false);
        } else {
            return this;
        }
    }
}
