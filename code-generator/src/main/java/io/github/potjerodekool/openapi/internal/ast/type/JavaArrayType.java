package io.github.potjerodekool.openapi.internal.ast.type;

import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;

public class JavaArrayType extends AbstractType<TypeElement> implements ArrayType {

    private final Type<?> componentType;

    public JavaArrayType(final Type<?> componentType) {
        super(TypeElement.ARRAY_ELEMENT);
        this.componentType = componentType;
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

        final var otherArrayType = (JavaArrayType) otherType;
        return componentType.isAssignableBy(otherArrayType.componentType);
    }

    @Override
    public boolean isSameType(final Type<?> otherType) {
        if (!otherType.isArrayType()) {
            return false;
        }

        final var otherArrayType = (JavaArrayType) otherType;
        return componentType.isSameType(otherArrayType.componentType);
    }
}
