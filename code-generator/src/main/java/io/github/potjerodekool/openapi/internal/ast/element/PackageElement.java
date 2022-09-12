package io.github.potjerodekool.openapi.internal.ast.element;

import io.github.potjerodekool.openapi.internal.ast.type.PackageType;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PackageElement extends AbstractElement<PackageElement, PackageType> implements QualifiedNameable {

    public static final PackageElement DEFAULT_PACKAGE = new PackageElement("");

    private PackageElement(final String name) {
        super(ElementKind.PACKAGE, name);
    }

    public static PackageElement create(final @Nullable String name) {
        if (name == null || name.isEmpty()) {
            return DEFAULT_PACKAGE;
        }

        final var pe = new PackageElement(name);
        final var type = new PackageType(pe);
        pe.setType(type);
        return pe;
    }

    @Override
    public String getQualifiedName() {
        return getSimpleName();
    }

    @Override
    public <R,P> R accept(final ElementVisitor<R, P> elementVisitor, final P param) {
        return elementVisitor.visitPackageElement(this, param);
    }

    public boolean isDefaultPackage() {
        return this == DEFAULT_PACKAGE;
    }
}
