package io.github.potjerodekool.openapi.internal.ast.type.kotlin;

import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;
import io.github.potjerodekool.openapi.internal.ast.type.AbstractType;
import io.github.potjerodekool.openapi.internal.ast.type.ArrayType;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import io.github.potjerodekool.openapi.internal.ast.type.TypeVisitor;

public class KotlinArray extends AbstractType<TypeElement> implements ArrayType {

    private final Type<?> componentType;

    public KotlinArray(final Type<?> componentType) {
        super(TypeElement.ARRAY_ELEMENT);
        this.componentType = componentType;
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
        if (otherType instanceof KotlinArray otherKotlinArray) {
            return componentType.isAssignableBy(otherKotlinArray.componentType);
        }
        return false;
    }

    @Override
    public boolean isSameType(final Type<?> otherType) {
        if (otherType instanceof KotlinArray otherKotlinArray) {
            return componentType.isSameType(otherKotlinArray.componentType);
        }
        return false;
    }
}
