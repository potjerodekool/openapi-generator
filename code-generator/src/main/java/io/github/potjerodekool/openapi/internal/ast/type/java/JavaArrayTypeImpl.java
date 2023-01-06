package io.github.potjerodekool.openapi.internal.ast.type.java;

import io.github.potjerodekool.openapi.internal.ast.element.AnnotationMirror;
import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import io.github.potjerodekool.openapi.internal.ast.type.TypeVisitor;

import java.util.List;
import java.util.Optional;

public class JavaArrayTypeImpl implements io.github.potjerodekool.openapi.internal.ast.type.JavaArrayType {

    private final Type<?> componentType;
    private final TypeElement element;
    private final boolean isNullable;

    public JavaArrayTypeImpl(final Type<?> componentType,
                             final TypeElement arrayElement,
                             final boolean isNullable) {
        this.componentType = componentType;
        this.element = arrayElement;
        this.isNullable = isNullable;
    }

    @Override
    public TypeElement getElement() {
        return element;
    }

    @Override
    public List<AnnotationMirror> getAnnotations() {
        return List.of();
    }

    @Override
    public Optional<List<? extends Type<?>>> getTypeArguments() {
        return Optional.empty();
    }

    @Override
    public Type<?> getComponentType() {
        return componentType;
    }

    @Override
    public <R, P> R accept(final TypeVisitor<R, P> visitor,
                           final P param) {
        return visitor.visitJavaArrayType(this, param);
    }

    @Override
    public boolean isAssignableBy(final Type<?> otherType) {
        if (!otherType.isArrayType()) {
            return false;
        }

        final var otherArrayType = (io.github.potjerodekool.openapi.internal.ast.type.JavaArrayType) otherType;
        return componentType.isAssignableBy(otherArrayType.getComponentType());
    }

    @Override
    public boolean isSameType(final Type<?> otherType) {
        if (!otherType.isArrayType()) {
            return false;
        }

        final var otherArrayType = (io.github.potjerodekool.openapi.internal.ast.type.JavaArrayType) otherType;
        return componentType.isSameType(otherArrayType.getComponentType());
    }

    @Override
    public Type<?> asNullableType() {
        return isNullable ? this : new JavaArrayTypeImpl(componentType, element, true);
    }

    @Override
    public Type<?> asNonNullableType() {
        return !isNullable ? this : new JavaArrayTypeImpl(componentType, element, false);
    }

    @Override
    public boolean isNullable() {
        return isNullable;
    }
}
