package io.github.potjerodekool.openapi.internal.ast.element;

import io.github.potjerodekool.openapi.internal.ast.type.ExecutableType;

public abstract class ExecutableElement extends AbstractElement<ExecutableElement, ExecutableType> {

    protected ExecutableElement(final ElementKind kind,
                                final String simpleName) {
        super(kind, simpleName);
    }
}
