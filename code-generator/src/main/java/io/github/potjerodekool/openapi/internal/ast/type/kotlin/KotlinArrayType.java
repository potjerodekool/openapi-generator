package io.github.potjerodekool.openapi.internal.ast.type.kotlin;

import io.github.potjerodekool.openapi.internal.ast.element.AnnotationMirror;
import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;
import io.github.potjerodekool.openapi.internal.ast.type.ArrayType;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import io.github.potjerodekool.openapi.internal.ast.type.TypeVisitor;

import java.util.List;
import java.util.Optional;

public class KotlinArrayType implements ArrayType {

    private final Type<?> componentType;
    private final TypeElement element;
    private final boolean isNullable;

    public KotlinArrayType(final Type<?> componentType,
                           final TypeElement element,
                           final boolean isNullable) {
        this.componentType = componentType;
        this.element = element;
        this.isNullable = isNullable;
    }

    @Override
    public TypeElement getElement() {
        return element;
    }

    @Override
    public Optional<List<? extends Type<?>>> getTypeArguments() {
        return Optional.empty();
    }

    @Override
    public List<AnnotationMirror> getAnnotations() {
        return List.of();
    }

    @Override
    public Type<?> getComponentType() {
        return componentType;
    }

    @Override
    public <R, P> R accept(final TypeVisitor<R, P> visitor, final P param) {
        return visitor.visitKotlinArray(this, param);
    }

    @Override
    public boolean isAssignableBy(final Type<?> otherType) {
        if (otherType instanceof KotlinArrayType otherKotlinArrayType) {
            return componentType.isAssignableBy(otherKotlinArrayType.componentType);
        }
        return false;
    }

    @Override
    public boolean isSameType(final Type<?> otherType) {
        if (otherType instanceof KotlinArrayType otherKotlinArrayType) {
            return componentType.isSameType(otherKotlinArrayType.componentType);
        }
        return false;
    }

    @Override
    public Type<?> asNullableType() {
        return isNullable ? this : new KotlinArrayType(componentType, element, true);
    }

    @Override
    public Type<?> asNonNullableType() {
        return !isNullable ? this : new KotlinArrayType(componentType, element, false);
    }

    @Override
    public boolean isNullable() {
        return isNullable;
    }
}
