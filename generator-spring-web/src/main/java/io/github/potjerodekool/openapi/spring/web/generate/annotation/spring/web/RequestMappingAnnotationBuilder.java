package io.github.potjerodekool.openapi.spring.web.generate.annotation.spring.web;

import io.github.potjerodekool.openapi.common.generate.annotation.AbstractAnnotationBuilder;

import java.util.List;

public class RequestMappingAnnotationBuilder extends AbstractAnnotationBuilder<RequestMappingAnnotationBuilder> {

    public static RequestMappingAnnotationBuilder post() {
        return new RequestMappingAnnotationBuilder("org.springframework.web.bind.annotation.PostMapping");
    }

    public static RequestMappingAnnotationBuilder get() {
        return new RequestMappingAnnotationBuilder("org.springframework.web.bind.annotation.GetMapping");
    }

    public static RequestMappingAnnotationBuilder put() {
        return new RequestMappingAnnotationBuilder("org.springframework.web.bind.annotation.PutMapping");
    }

    public static RequestMappingAnnotationBuilder patch() {
        return new RequestMappingAnnotationBuilder("org.springframework.web.bind.annotation.PatchMapping");
    }

    public static RequestMappingAnnotationBuilder delete() {
        return new RequestMappingAnnotationBuilder("org.springframework.web.bind.annotation.DeleteMapping");
    }

    private RequestMappingAnnotationBuilder(final String annotationClassName) {
        super(annotationClassName);
    }

    public RequestMappingAnnotationBuilder value(final String... value) {
        return addStringArray("value", value);
    }

    public RequestMappingAnnotationBuilder consumes(final String... consumes) {
        return addStringArray("consumes", consumes);
    }

    public RequestMappingAnnotationBuilder consumes(final List<String> consumes) {
        return addStringArray("consumes", consumes);
    }

    public RequestMappingAnnotationBuilder produces(final String... produces) {
        return addStringArray("produces", produces);
    }

    public RequestMappingAnnotationBuilder produces(final List<String> produces) {
        return addStringArray("produces", produces);
    }

}
