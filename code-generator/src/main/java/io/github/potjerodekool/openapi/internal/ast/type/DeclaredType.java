package io.github.potjerodekool.openapi.internal.ast.type;

import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;
import java.util.List;

public interface DeclaredType extends MutableType<TypeElement> {

    DeclaredType asNullableType();

    @Override
    DeclaredType withTypeArguments(final List<? extends Type<?>> typeArguments);

    @Override
    default DeclaredType withTypeArgument(final Type<?> typeArgument) {
        return withTypeArguments(List.of(typeArgument));
    }

    @Override
    default DeclaredType withTypeArguments(final Type<?> first,
                                           final Type<?> second) {
        return withTypeArguments(List.of(first, second));
    }

    @Override
    DeclaredType withTypeArguments();

    DeclaredType copy();
}
