package io.github.potjerodekool.openapi.common.generate.annotation;

import io.github.potjerodekool.codegen.template.model.annotation.Annot;
import io.github.potjerodekool.codegen.template.model.expression.*;
import io.github.potjerodekool.codegen.template.model.type.ClassOrInterfaceTypeExpr;

import java.util.*;

public abstract class AbstractAnnotationBuilder<B extends AbstractAnnotationBuilder<B>> {

    private final String annotationClassName;

    private final Map<String, Expr> members = new HashMap<>();

    protected AbstractAnnotationBuilder(final String annotationClassName) {
        this.annotationClassName = annotationClassName;
    }

    protected B add(final String name,
                    final Expr annotationValue) {
        if (annotationValue != null) {
            members.put(name, annotationValue);
        }
        return (B) this;
    }

    protected B addEnumAttribute(final String name,
                                 final String annotationClassName,
                                 final String valueName) {
        Objects.requireNonNull(annotationClassName, "annotationClassName is required");
        Objects.requireNonNull(valueName, "valueName is required");

        members.put(name, new FieldAccessExpr()
                .target(new ClassOrInterfaceTypeExpr(annotationClassName))
                .field(new IdentifierExpr(valueName))
        );
        return (B) this;
    }

    protected B add(final String name,
                    final String value) {
        if (value != null) {
            members.put(name, new SimpleLiteralExpr(value));
        }
        return (B) this;
    }

    protected B add(final String name,
                    final Boolean value) {
        if (value != null) {
            members.put(name, new SimpleLiteralExpr(value));
        }
        return (B) this;
    }

    protected B addStringArray(final String name,
                               final List<String> values) {
        if (values != null && !values.isEmpty()) {
            final var literals = values.stream()
                    .map(v -> (Expr) new SimpleLiteralExpr(v))
                    .toList();
            return addAttributeArray(name, literals);
        }
        return (B) this;
    }

    protected B addStringArray(final String name,
                               final String... values) {
        if (values != null && values.length > 0) {
            final var literals = Arrays.stream(values)
                    .map(v -> (Expr) new SimpleLiteralExpr(v))
                    .toList();
            return addAttributeArray(name, literals);
        }
        return (B) this;
    }

    protected <E extends Expr> B addAttributeArray(final String name,
                                                   final List<E> values) {
        if (values != null && !values.isEmpty()) {
            members.put(name, new ArrayExpr().values(values));
        }
        return (B) this;
    }

    protected B addAttributeArray(final String name,
                                  final ArrayExpr value) {
        if (value != null && !value.getValues().isEmpty()) {
            members.put(name, value);
        }
        return (B) this;
    }

    protected B addCompoundArray(final String name,
                                 final List<? extends Expr> value) {
        if (value != null && !value.isEmpty()) {
            members.put(name, new ArrayExpr().values(value));
        }
        return (B) this;
    }

    public Annot build() {
        final var annot = new Annot(annotationClassName);
        members.forEach((annot::value));
        return annot;
    }
}
