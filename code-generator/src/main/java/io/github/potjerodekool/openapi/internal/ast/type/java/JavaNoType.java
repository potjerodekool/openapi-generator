package io.github.potjerodekool.openapi.internal.ast.type.java;

import io.github.potjerodekool.openapi.internal.ast.element.*;
import io.github.potjerodekool.openapi.internal.ast.type.NoType;
import io.github.potjerodekool.openapi.internal.ast.type.Type;

import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public abstract class JavaNoType implements NoType {

    private final Element element;

    protected JavaNoType() {
        this.element = TypeElement.create(ElementKind.OTHER, List.of(), Set.of(), "");
    }

    @Override
    public Element getElement() {
        return element;
    }

    @Override
    public List<AnnotationMirror> getAnnotations() {
        return List.of();
    }

    @Override
    public Optional<List<? extends Type<?>>> getTypeArguments() {
        return Optional.empty();
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.NONE;
    }


    @Override
    public Type<?> asNullableType() {
        return this;
    }

    @Override
    public Type<?> asNonNullableType() {
        return this;
    }
}
