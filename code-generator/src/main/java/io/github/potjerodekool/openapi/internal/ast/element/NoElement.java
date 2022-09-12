package io.github.potjerodekool.openapi.internal.ast.element;

import io.github.potjerodekool.openapi.internal.ast.type.NoType;

public class NoElement extends AbstractElement<NoElement, NoType> {

    public static final NoElement INSTANCE = new NoElement();

    private NoElement() {
        super(ElementKind.OTHER, "no");
    }

    @Override
    public <R, P> R accept(final ElementVisitor<R, P> elementVisitor, final P param) {
        return elementVisitor.visitUnknown(this, param);
    }
}
