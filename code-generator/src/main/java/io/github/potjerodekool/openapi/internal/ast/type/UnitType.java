package io.github.potjerodekool.openapi.internal.ast.type;

public class UnitType extends NoType {

    @Override
    public <R, P> R accept(final TypeVisitor<R, P> visitor, final P param) {
        return visitor.visitUnitType(this, param);
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
