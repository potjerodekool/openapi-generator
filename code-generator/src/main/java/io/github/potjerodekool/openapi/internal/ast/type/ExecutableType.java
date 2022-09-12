package io.github.potjerodekool.openapi.internal.ast.type;

import io.github.potjerodekool.openapi.internal.ast.element.MethodElement;

public class ExecutableType extends AbstractType<MethodElement> {

    public ExecutableType(final MethodElement element) {
        super(element);
    }

    @Override
    public <R,P> R accept(final TypeVisitor<R, P> visitor,
                          final P param) {
        return visitor.visitExecutableType(this, param);
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.EXECUTABLE;
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
