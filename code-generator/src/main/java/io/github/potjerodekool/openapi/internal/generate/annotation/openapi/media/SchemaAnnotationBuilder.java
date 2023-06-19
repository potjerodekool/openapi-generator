package io.github.potjerodekool.openapi.internal.generate.annotation.openapi.media;

import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.tree.expression.LiteralExpression;
import io.github.potjerodekool.codegen.model.tree.type.TypeExpression;
import io.github.potjerodekool.openapi.internal.generate.annotation.AbstractAnnotationBuilder;

public class SchemaAnnotationBuilder extends AbstractAnnotationBuilder<SchemaAnnotationBuilder> {

    public SchemaAnnotationBuilder() {
        super("io.swagger.v3.oas.annotations.media.Schema");
    }

    public SchemaAnnotationBuilder implementation(final TypeExpression implementationClass) {
        return add("implementation", LiteralExpression.createClassLiteralExpression(implementationClass));
    }

    public SchemaAnnotationBuilder requiredMode(final Boolean required) {
        final var requiredMode = Boolean.TRUE.equals(required)
                ? "REQUIRED"
                : Boolean.FALSE.equals(required)
                ? "NOT_REQUIRED" : "AUTO";

        return addEnumAttribute("requiredMode", "io.swagger.v3.oas.annotations.media.Schema.RequiredMode", Name.of(requiredMode));
    }

    public SchemaAnnotationBuilder type(final String type) {
        return add("type", LiteralExpression.createStringLiteralExpression(type));
    }

    public SchemaAnnotationBuilder format(final String format) {
        return add("format",format);
    }

    public SchemaAnnotationBuilder description(final String description) {
       return add("description", description);
    }

    public SchemaAnnotationBuilder accessMode(final Boolean readOnly,
                                              final Boolean writeOnly) {
        final String accessMode;

        if (Boolean.TRUE.equals(readOnly)) {
            accessMode = "READ_ONLY";
        } else if (Boolean.TRUE.equals(writeOnly)) {
            accessMode = "WRITE_ONLY";
        } else if (readOnly != null && writeOnly != null) {
            accessMode = "READ_WRITE";
        } else {
            accessMode = "AUTO";
        }

        return addEnumAttribute("accessMode", "io.swagger.v3.oas.annotations.media.Schema$AccessMode", Name.of(accessMode));
    }
}
