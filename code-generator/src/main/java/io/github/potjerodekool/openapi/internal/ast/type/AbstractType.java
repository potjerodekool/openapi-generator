package io.github.potjerodekool.openapi.internal.ast.type;

import io.github.potjerodekool.openapi.internal.ast.element.AnnotationMirror;
import io.github.potjerodekool.openapi.internal.ast.element.Element;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@SuppressWarnings("initialization.fields.uninitialized")
public abstract class AbstractType<E extends Element> implements MutableType<E> {

    private final E element;

    private final List<AnnotationMirror> annotations = new ArrayList<>();

    private final @Nullable List<? extends Type<?>> typeArguments;

    protected AbstractType(final E element) {
        this(element, new ArrayList<>(), null);
    }

    @SuppressWarnings("method.invocation")
    protected AbstractType(final E element,
                           final List<AnnotationMirror> annotations,
                           @Nullable List<? extends Type<?>> typeArguments) {
        this.element = element;
        this.annotations.addAll(annotations);
        this.typeArguments = typeArguments;
        validateAnnotations();
    }

    @Override
    public E getElement() {
        return element;
    }

    @Override
    public List<AnnotationMirror> getAnnotations() {
        return annotations;
    }

    @Override
    public void addAnnotation(final AnnotationMirror annotation) {
        this.annotations.add(annotation);
        validateAnnotations();
    }

    private void validateAnnotations() {
        final var map = this.annotations.stream()
                .collect(Collectors.groupingBy(
                        AnnotationMirror::getAnnotationClassName
                ));

        map.forEach((k,v) -> {
            if (v.size() > 1) {
                throw new IllegalArgumentException("Duplicate annotation");
            }
        });
    }

    public Optional<List<? extends Type<?>>> getTypeArguments() {
        return Optional.ofNullable(typeArguments);
    }

    @Override
    public Type<?> withTypeArguments(final List<? extends Type<?>> typeArguments) {
        return this;
    }

    @Override
    public Type<?> withTypeArgument(final Type<?> typeArgument) {
        return this;
    }

    @Override
    public Type<?> withTypeArguments(final Type<?> first, final Type<?> second) {
        return this;
    }

    @Override
    public Type<?> withTypeArguments() {
        return this;
    }
}
