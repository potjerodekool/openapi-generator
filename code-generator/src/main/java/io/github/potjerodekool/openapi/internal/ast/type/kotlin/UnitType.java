package io.github.potjerodekool.openapi.internal.ast.type.kotlin;

import io.github.potjerodekool.openapi.internal.ast.type.java.JavaNoType;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import io.github.potjerodekool.openapi.internal.ast.type.TypeVisitor;

public class UnitType extends JavaNoType {

    public static final UnitType INSTANCE = new UnitType();

    private UnitType() {}

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
