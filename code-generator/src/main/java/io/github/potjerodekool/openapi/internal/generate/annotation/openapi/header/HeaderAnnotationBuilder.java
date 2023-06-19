package io.github.potjerodekool.openapi.internal.generate.annotation.openapi.header;

import io.github.potjerodekool.codegen.model.tree.expression.Expression;
import io.github.potjerodekool.openapi.internal.generate.annotation.AbstractAnnotationBuilder;

public class HeaderAnnotationBuilder extends AbstractAnnotationBuilder<HeaderAnnotationBuilder> {

    public HeaderAnnotationBuilder() {
        super("io.swagger.v3.oas.annotations.headers.Header");
    }

    public HeaderAnnotationBuilder name(final String name) {
        return add("name", name);
    }

    public HeaderAnnotationBuilder description(final String description) {
        return add("description", description);
    }

    public HeaderAnnotationBuilder required(final Boolean required) {
        return add("required", required);
    }

    public HeaderAnnotationBuilder deprecated(final Boolean deprecated) {
        return add("deprecated", deprecated);
    }

    public HeaderAnnotationBuilder schema(final Expression schema) {
        return add("schema", schema);
    }

}
