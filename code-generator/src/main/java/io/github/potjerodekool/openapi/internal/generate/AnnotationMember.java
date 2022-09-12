package io.github.potjerodekool.openapi.internal.generate;

import io.github.potjerodekool.openapi.internal.ast.expression.*;
import io.github.potjerodekool.openapi.internal.ast.type.Type;

public class AnnotationMember {

    private final String name;
    private final Expression value;

    public AnnotationMember(final String name,
                            final String value) {
        this(name, LiteralExpression.createStringLiteralExpression(value));
    }

    public AnnotationMember(final String name,
                            final boolean value) {
        this(name, LiteralExpression.createBooleanLiteralExpression(value));
    }

    public AnnotationMember(final String name,
                            final Type<?> value) {
        this(name, LiteralExpression.createClassLiteralExpression(value));
    }

    public AnnotationMember(final String name,
                            final LiteralExpression value) {
        this.name = name;
        this.value = value;
    }

    public AnnotationMember(final String name,
                            final AnnotationExpression value) {
        this.name = name;
        this.value = value;
    }

    public AnnotationMember(final String name,
                            final ArrayInitializerExpression value) {
        this.name = name;
        this.value = value;
    }

    public AnnotationMember(final String name,
                            final FieldAccessExpression value) {
        this.name = name;
        this.value = value;
    }

    public String name() {
        return name;
    }

    public Expression value() {
        return value;
    }
}

