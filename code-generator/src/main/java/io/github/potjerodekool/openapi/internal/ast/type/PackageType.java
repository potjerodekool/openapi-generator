package io.github.potjerodekool.openapi.internal.ast.type;

import io.github.potjerodekool.openapi.internal.ast.element.PackageElement;

public class PackageType extends AbstractType<PackageElement> {

    public PackageType(final PackageElement packageElement) {
        super(packageElement);
    }

    @Override
    public <R,P> R accept(final TypeVisitor<R,P> visitor,
                          final P param) {
        return visitor.visitPackageType(this, param);
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.PACKAGE;
    }

    @Override
    public boolean isAssignableBy(final Type<?> otherType) {
        return false;
    }

    @Override
    public boolean isSameType(final Type<?> fieldType) {
        return false;
    }
}
