package io.github.potjerodekool.openapi.internal.ast.type;

import javax.lang.model.type.TypeKind;

public interface ErrorType extends DeclaredType {

    @Override
    default TypeKind getKind() {
        return TypeKind.ERROR;
    }
}
