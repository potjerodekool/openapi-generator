package io.github.potjerodekool.openapi.internal.ast.type;

import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;

public class ErrorType extends DeclaredType {

    public ErrorType(final TypeElement typeElement) {
        super(typeElement, false);
    }

    @Override
    public <R, P> R accept(final TypeVisitor<R, P> visitor, final P param) {
        return visitor.visitUnknownType(this, param);
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.ERROR;
    }

    @Override
    public boolean isAssignableBy(final Type<?> otherType) {
        return false;
    }

    @Override
    public boolean isSameType(final Type<?> otherType) {
        return false;
    }
}
