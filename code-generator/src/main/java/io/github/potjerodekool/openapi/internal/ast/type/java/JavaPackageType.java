package io.github.potjerodekool.openapi.internal.ast.type.java;

import io.github.potjerodekool.openapi.internal.ast.element.AnnotationMirror;
import io.github.potjerodekool.openapi.internal.ast.element.PackageElement;
import io.github.potjerodekool.openapi.internal.ast.type.PackageType;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import io.github.potjerodekool.openapi.internal.ast.type.TypeVisitor;

import javax.lang.model.type.TypeKind;
import java.util.List;
import java.util.Optional;

public class JavaPackageType implements PackageType {

    private final PackageElement element;

    public JavaPackageType(final PackageElement packageElement) {
        this.element = packageElement;
    }

    @Override
    public PackageElement getElement() {
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
    public <R,P> R accept(final TypeVisitor<R,P> visitor,
                          final P param) {
        return visitor.visitPackageType(this, param);
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.PACKAGE;
    }

    @Override
    public boolean isAssignableBy(final Type<?> otherType) {
        return false;
    }

    @Override
    public boolean isSameType(final Type<?> fieldType) {
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
