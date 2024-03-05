package io.github.potjerodekool.openapi.common.generate.annotation.openapi.media;

import io.github.potjerodekool.codegen.template.model.expression.ClassLiteralExpr;
import io.github.potjerodekool.codegen.template.model.expression.LiteralExpr;
import io.github.potjerodekool.codegen.template.model.expression.SimpleLiteralExpr;
import io.github.potjerodekool.codegen.template.model.type.ArrayTypeExpr;
import io.github.potjerodekool.codegen.template.model.type.ClassOrInterfaceTypeExpr;
import io.github.potjerodekool.codegen.template.model.type.PrimitiveTypeExpr;
import io.github.potjerodekool.codegen.template.model.type.TypeExpr;
import io.github.potjerodekool.openapi.common.generate.annotation.AbstractAnnotationBuilder;

import java.util.List;

public class SchemaAnnotationBuilder extends AbstractAnnotationBuilder<SchemaAnnotationBuilder> {

    public SchemaAnnotationBuilder() {
        super("io.swagger.v3.oas.annotations.media.Schema");
    }

    public SchemaAnnotationBuilder implementation(final TypeExpr implementationClass) {
        if (implementationClass == null) {
            return this;
        }

        final var className = switch (implementationClass) {
            case ClassOrInterfaceTypeExpr classOrInterfaceTypeExpr -> classOrInterfaceTypeExpr.getName();
            case PrimitiveTypeExpr primitiveTypeExpr -> primitiveTypeExpr.getName();
            default -> throw new UnsupportedOperationException(implementationClass.getClass().getName());
        };

        return add("implementation", new ClassLiteralExpr(className));
    }

    public SchemaAnnotationBuilder requiredMode(final Boolean required) {
        final var requiredMode = Boolean.TRUE.equals(required)
                ? "REQUIRED"
                : Boolean.FALSE.equals(required)
                ? "NOT_REQUIRED" : "AUTO";

        return addEnumAttribute(
                "requiredMode",
                "io.swagger.v3.oas.annotations.media.Schema.RequiredMode",
                requiredMode);
    }

    public SchemaAnnotationBuilder type(final String type) {
        return add("type", new SimpleLiteralExpr(type));
    }

    public SchemaAnnotationBuilder format(final String format) {
        return add("format", format);
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

        return addEnumAttribute(
                "accessMode",
                "io.swagger.v3.oas.annotations.media.Schema.AccessMode",
                accessMode
        );
    }

    public SchemaAnnotationBuilder nullable(final Boolean nullable) {
        if (nullable != null) {
            add("nullable", nullable);
        }
        return this;
    }

    public <LE extends LiteralExpr> SchemaAnnotationBuilder requiredProperties(final List<LE> requiredProperties) {
        return addCompoundArray("requiredProperties", requiredProperties);
    }

    public SchemaAnnotationBuilder name(final String name) {
        return add("name", name);
    }

    public SchemaAnnotationBuilder title(final String title) {
        return add("title", title);
    }

    public SchemaAnnotationBuilder extensions(final ArrayTypeExpr extensions) {
        return add("extensions", extensions);
    }
}
