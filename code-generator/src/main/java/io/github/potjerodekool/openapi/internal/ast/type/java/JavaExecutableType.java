package io.github.potjerodekool.openapi.internal.ast.type.java;

import io.github.potjerodekool.openapi.internal.ast.element.AnnotationMirror;
import io.github.potjerodekool.openapi.internal.ast.element.MethodElement;
import io.github.potjerodekool.openapi.internal.ast.type.ExecutableType;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import io.github.potjerodekool.openapi.internal.ast.type.TypeVisitor;

import javax.lang.model.type.TypeKind;
import java.util.List;
import java.util.Optional;

public class JavaExecutableType implements ExecutableType {

    private final MethodElement element;

    public JavaExecutableType(final MethodElement element) {
        this.element = element;
    }

    @Override
    public MethodElement getElement() {
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
    public <R,P> R accept(final TypeVisitor<R, P> visitor,
                          final P param) {
        return visitor.visitExecutableType(this, param);
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.EXECUTABLE;
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
    public Type<?> asNullableType() {
        return this;
    }

    @Override
    public Type<?> asNonNullableType() {
        return this;
    }
}
