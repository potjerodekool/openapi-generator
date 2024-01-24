package io.github.potjerodekool.openapi.internal.generate.model.model.element;

import io.github.potjerodekool.openapi.internal.generate.model.model.expresion.LiteralExpression;

import java.util.Map;

public class Annotation {

    private String name;

    private Map<String, Object> attributes;

    private AnnotationTarget annotationTarget;

    public String getName() {
        return name;
    }

    public Annotation name(final String name) {
        this.name = name;
        return this;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public Annotation attributes(final Map<String, Object> attributes) {
        if (this.attributes == null) {
            this.attributes = new java.util.LinkedHashMap<>();
        }

        attributes.forEach((key, value) -> this.attributes.put(key, transform(value)));

        this.attributes = attributes;
        return this;
    }

    public Annotation attribute(final String name, final Object value) {
        if (this.attributes == null) {
            this.attributes = new java.util.LinkedHashMap<>();
        }
        this.attributes.put(name, transform(value));
        return this;
    }

    private Object transform(final Object value) {
        if (value instanceof String s) {
            return new LiteralExpression(s);
        }

        return value;
    }

    public AnnotationTarget getAnnotationTarget() {
        return annotationTarget;
    }

    public Annotation annotationTarget(final AnnotationTarget annotationTarget) {
        this.annotationTarget = annotationTarget;
        return this;
    }
}
