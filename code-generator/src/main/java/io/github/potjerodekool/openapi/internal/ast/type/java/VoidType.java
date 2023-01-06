package io.github.potjerodekool.openapi.internal.ast.type.java;

import io.github.potjerodekool.openapi.internal.ast.type.Type;
import io.github.potjerodekool.openapi.internal.ast.type.TypeVisitor;

import javax.lang.model.type.TypeKind;

public class VoidType extends JavaNoType {

    public static final VoidType INSTANCE = new VoidType();

    private VoidType() {
    }

    @Override
    public <R,P> R accept(final TypeVisitor<R,P> visitor,
                          final P param) {
        return visitor.visitVoidType(this, param);
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.VOID;
    }

    @Override
    public boolean isAssignableBy(final Type<?> otherType) {
        return otherType == this;
    }

    @Override
    public boolean isSameType(final Type<?> otherType) {
        return otherType == this;
    }

    @Override
    public boolean isVoidType() {
        return true;
    }

}
