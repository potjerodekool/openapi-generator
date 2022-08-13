package io.github.potjerodekool.openapi.internal.generate;

import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.ClassExpr;

public class AnnotationMember {

    private final String name;
    private final Object value;
    private final AnnotationMemberType type;

    public AnnotationMember(final String name,
                            final String value) {
        this(name, value, AnnotationMemberType.STRING_LITERAL);
    }

    public AnnotationMember(final String name,
                            final boolean value) {
        this(name, value, AnnotationMemberType.BOOLEAN_LITERAL);
    }

    public AnnotationMember(final String name,
                            final int value) {
        this(name, value, AnnotationMemberType.INTEGER_LITERAL);
    }

    public AnnotationMember(final String name,
                            final AnnotationExpr value) {
        this(name, value, AnnotationMemberType.ANNOTATION_EXPRESSION);
    }

    public AnnotationMember(final String name,
                            final ClassExpr value) {
        this(name, value, AnnotationMemberType.CLASS_EXPRESSION);
    }

    public AnnotationMember(final String name,
                            final ArrayInitializerExpr value) {
        this(name, value, AnnotationMemberType.ARRAY_EXPRESSION);
    }

    private AnnotationMember(final String name,
                             final Object value,
                             final AnnotationMemberType type) {
        this.name = name;
        this.value = value;
        this.type = type;
    }

    public String name() {
        return name;
    }

    public Object value() {
        return value;
    }

    public AnnotationMemberType type() {
        return type;
    }
}

enum AnnotationMemberType {

    STRING_LITERAL,
    BOOLEAN_LITERAL,
    INTEGER_LITERAL,
    ANNOTATION_EXPRESSION,
    CLASS_EXPRESSION,
    ARRAY_EXPRESSION
}
