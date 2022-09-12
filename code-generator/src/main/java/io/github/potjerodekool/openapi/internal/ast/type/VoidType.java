package io.github.potjerodekool.openapi.internal.ast.type;

public class VoidType extends NoType {

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
