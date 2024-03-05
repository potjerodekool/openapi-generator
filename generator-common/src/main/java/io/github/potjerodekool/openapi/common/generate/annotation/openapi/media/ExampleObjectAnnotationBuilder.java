package io.github.potjerodekool.openapi.common.generate.annotation.openapi.media;

import io.github.potjerodekool.openapi.common.generate.annotation.AbstractAnnotationBuilder;

public class ExampleObjectAnnotationBuilder extends AbstractAnnotationBuilder<ExampleObjectAnnotationBuilder> {

    public ExampleObjectAnnotationBuilder() {
        super("io.swagger.v3.oas.annotations.media.ExampleObject");
    }

    public ExampleObjectAnnotationBuilder name(final String name) {
        return add("name", name);
    }

    public ExampleObjectAnnotationBuilder summary(final String summary) {
        return add("summary", summary);
    }

    public ExampleObjectAnnotationBuilder value(final String value) {
        return add("value", value);
    }
}
