package io.github.potjerodekool.openapi.internal.ast.type;

import io.github.potjerodekool.openapi.internal.ast.element.Element;
import io.github.potjerodekool.openapi.internal.ast.expression.AnnotationExpression;

import java.util.List;

public interface Type<E extends Element> {

    List<AnnotationExpression> getAnnotations();

    <R, P> R accept(TypeVisitor<R, P> visitor,
                    P param);

    E getElement();

    TypeKind getKind();

    default boolean isPrimitiveType() {
        return false;
    }

    default boolean isArrayType() {
        return false;
    }

    default boolean isDeclaredType() {
        return false;
    }

    default boolean isWildCardType() { return false; }

    default boolean isVoidType() { return false; }

    void addAnnotation(AnnotationExpression annotation);

    boolean isAssignableBy(final Type<?> otherType);

    boolean isSameType(Type<?> otherType);

    default boolean isNullable() {
        return false;
    }
}
