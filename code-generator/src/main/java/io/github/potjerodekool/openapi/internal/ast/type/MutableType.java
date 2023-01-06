package io.github.potjerodekool.openapi.internal.ast.type;

import io.github.potjerodekool.openapi.internal.ast.Attribute;
import io.github.potjerodekool.openapi.internal.ast.element.AnnotationMirror;
import io.github.potjerodekool.openapi.internal.ast.element.Element;

import java.util.List;

public interface MutableType<E extends Element> extends Type<E> {

    void addAnnotation(AnnotationMirror annotation);

    default void addAnnotation(final String name) {
        addAnnotation(Attribute.compound(name));
    }

    Type<?> withTypeArguments(final List<? extends Type<?>> typeArguments);

    default Type<?> withTypeArgument(final Type<?> typeArgument) {
        return withTypeArguments(List.of(typeArgument));
    }

    default Type<?> withTypeArguments(final Type<?> first,
                                      final Type<?> second) {
        return withTypeArguments(List.of(first, second));
    }

    Type<?> withTypeArguments();
}
