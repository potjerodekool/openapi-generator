package io.github.potjerodekool.openapi.internal.ast.type;

import io.github.potjerodekool.openapi.internal.ast.element.Element;
import io.github.potjerodekool.openapi.internal.ast.element.NoElement;

public abstract class NoType extends AbstractType<Element> {

    protected NoType() {
        super(NoElement.INSTANCE);
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.NONE;
    }
}
