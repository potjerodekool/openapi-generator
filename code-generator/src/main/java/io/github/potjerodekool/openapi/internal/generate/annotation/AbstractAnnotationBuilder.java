package io.github.potjerodekool.openapi.internal.generate.annotation;

import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.expression.ArrayInitializerExpression;
import io.github.potjerodekool.codegen.model.tree.expression.Expression;
import io.github.potjerodekool.codegen.model.tree.expression.FieldAccessExpression;
import io.github.potjerodekool.codegen.model.tree.expression.LiteralExpression;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;

import java.util.*;

public abstract class AbstractAnnotationBuilder<B extends AbstractAnnotationBuilder<B>> {

    private final String annotationClassName;

    private final Map<String, Expression> members = new HashMap<>();

    protected AbstractAnnotationBuilder(final String annotationClassName) {
        this.annotationClassName = annotationClassName;
    }

    protected B add(final String name,
                    final Expression annotationValue) {
        if (annotationValue != null) {
            members.put(name, annotationValue);
        }
        return (B) this;
    }

    protected B addEnumAttribute(final String name,
                                 final String annotationClassName,
                                 final Name valueName) {
        Objects.requireNonNull(annotationClassName, "annotationClassName is required");
        Objects.requireNonNull(valueName, "valueName is required");

        members.put(name, new FieldAccessExpression(
                new ClassOrInterfaceTypeExpression(annotationClassName),
                valueName.toString()
        ));
        return (B) this;
    }

    protected B add(final String name,
                    final String value) {
        if (value != null) {
            members.put(name,  LiteralExpression.createStringLiteralExpression(value));
        }
        return (B) this;
    }

    protected B add(final String name,
                    final Boolean value) {
        if (value != null) {
            members.put(name, LiteralExpression.createBooleanLiteralExpression(value));
        }
        return (B) this;
    }

    protected B addStringArray(final String name,
                               final List<String> value) {
        if (value != null && !value.isEmpty()) {
            final var literals = value.stream()
                    .map(v -> (Expression) LiteralExpression.createStringLiteralExpression(v))
                    .toList();
            return addAttributeArray(name, literals);
        }
        return (B) this;
    }

    protected B addStringArray(final String name,
                               final String... value) {
        if (value != null && value.length > 0) {
            final var literals = Arrays.stream(value)
                    .map(v -> (Expression) LiteralExpression.createStringLiteralExpression(v))
                    .toList();
            return addAttributeArray(name, literals);
        }
        return (B) this;
    }

    protected B addAttributeArray(final String name,
                                  final List<Expression> value) {
        if (value != null && !value.isEmpty()) {
            members.put(name, new ArrayInitializerExpression(value));
        }
        return (B) this;
    }

    protected B addAttributeArray(final String name,
                                  final ArrayInitializerExpression value) {
        if (value != null && !value.getValues().isEmpty()) {
            members.put(name, value);
        }
        return (B) this;
    }

    protected B addCompoundArray(final String name,
                                  final List<? extends Expression> value) {
        if (value != null && !value.isEmpty()) {
            members.put(name, new ArrayInitializerExpression(value));
        }
        return (B) this;
    }

    public AnnotationExpression build() {
        return new AnnotationExpression(
                new ClassOrInterfaceTypeExpression(annotationClassName),
                new HashMap<>(members)
        );
    }

}
