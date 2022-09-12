package io.github.potjerodekool.openapi.internal.ast.type;

import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;

public interface ArrayType extends Type<TypeElement> {

    @Override
    default boolean isArrayType() {
        return true;
    }

    @Override
    default TypeKind getKind() {
        return TypeKind.ARRAY;
    }

    Type<?> getComponentType();
}
