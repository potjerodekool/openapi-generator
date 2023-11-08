package io.github.potjerodekool.openapi.internal.generate.annotation.openapi.extension;

import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.openapi.internal.generate.annotation.AbstractAnnotationBuilder;

import java.util.List;

public class ExtensionAnnotationBuilder extends AbstractAnnotationBuilder<ExtensionAnnotationBuilder> {
    public ExtensionAnnotationBuilder() {
        super("io.swagger.v3.oas.annotations.extensions.Extension");
    }

    public ExtensionAnnotationBuilder name(final String name) {
        return add("name", name);
    }

    public ExtensionAnnotationBuilder properties(final List<AnnotationExpression> properties) {
        return addCompoundArray("properties", properties);
    }
}
