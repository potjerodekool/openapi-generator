package io.github.potjerodekool.openapi.internal.generate.annotation.openapi.extension;

import io.github.potjerodekool.openapi.internal.generate.annotation.AbstractAnnotationBuilder;

public class ExtensionPropertyAnnotationBuilder extends AbstractAnnotationBuilder<ExtensionPropertyAnnotationBuilder> {
    public ExtensionPropertyAnnotationBuilder() {
        super("io.swagger.v3.oas.annotations.extensions.ExtensionProperty");
    }

    public ExtensionPropertyAnnotationBuilder name(final String name) {
        return add("name", name);
    }

    public ExtensionPropertyAnnotationBuilder value(final String name) {
        return add("value", name);
    }
}
