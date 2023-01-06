package io.github.potjerodekool.openapi.internal.ast.type;

import io.github.potjerodekool.openapi.internal.ast.element.AnnotationMirror;
import io.github.potjerodekool.openapi.internal.ast.element.Element;

import javax.lang.model.type.TypeKind;
import java.util.List;
import java.util.Optional;

public interface Type<E extends Element> {

    List<AnnotationMirror> getAnnotations();

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

    boolean isAssignableBy(final Type<?> otherType);

    boolean isSameType(Type<?> otherType);

    default boolean isNullable() {
        return false;
    }

    Optional<List<? extends Type<?>>> getTypeArguments();

    default Optional<Type<?>> getFirstTypeArg() {
        return Optional.empty();
    }

    Type<?> asNullableType();

    Type<?> asNonNullableType();
}
