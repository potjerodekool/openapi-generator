package io.github.potjerodekool.openapi.internal.ast.type.java;

import io.github.potjerodekool.openapi.internal.ast.element.AnnotationMirror;
import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;
import io.github.potjerodekool.openapi.internal.ast.type.*;

import javax.lang.model.type.TypeKind;
import java.util.List;
import java.util.Optional;

public class JavaErrorType implements ErrorType {

    private final TypeElement typeElement;
    private final boolean isNullable;

    public JavaErrorType(final TypeElement typeElement) {
        this.typeElement = typeElement;
        this.isNullable = false;
    }

    @Override
    public boolean isNullable() {
        return isNullable;
    }

    @Override
    public TypeElement getElement() {
        return typeElement;
    }

    @Override
    public Optional<List<? extends Type<?>>> getTypeArguments() {
        return Optional.empty();
    }

    @Override
    public JavaErrorType withTypeArguments() {
        return this;
    }

    @Override
    public JavaErrorType withTypeArguments(final List<? extends Type<?>> typeArguments) {
        return this;
    }

    @Override
    public void addAnnotation(final AnnotationMirror annotation) {
    }

    @Override
    public List<AnnotationMirror> getAnnotations() {
        return List.of();
    }

    @Override
    public JavaErrorType asNullableType() {
        return this;
    }

    @Override
    public Type<?> asNonNullableType() {
        return this;
    }

    @Override
    public <R, P> R accept(final TypeVisitor<R, P> visitor, final P param) {
        return visitor.visitUnknownType(this, param);
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.ERROR;
    }

    @Override
    public boolean isAssignableBy(final Type<?> otherType) {
        return false;
    }

    @Override
    public boolean isSameType(final Type<?> otherType) {
        return false;
    }

    @Override
    public DeclaredType copy() {
        return new JavaErrorType(typeElement);
    }
}
