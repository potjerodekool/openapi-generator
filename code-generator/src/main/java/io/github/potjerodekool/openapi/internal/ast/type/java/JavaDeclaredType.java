package io.github.potjerodekool.openapi.internal.ast.type.java;

import io.github.potjerodekool.openapi.internal.ast.element.AnnotationMirror;
import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;
import io.github.potjerodekool.openapi.internal.ast.type.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.lang.model.type.TypeKind;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JavaDeclaredType extends AbstractType<TypeElement> implements DeclaredType {

    private final boolean isNullable;

    public JavaDeclaredType(final TypeElement typeElement,
                            final boolean isNullable) {
        super(typeElement);
        this.isNullable = isNullable;
    }

    public JavaDeclaredType(final TypeElement typeElement,
                            final List<AnnotationMirror> annotations,
                            final @Nullable List<? extends Type<?>> typeArguments,
                            final boolean isNullable) {
        super(typeElement, annotations, typeArguments);
        this.isNullable = isNullable;
    }

    @Override
    public <R,P> R accept(final TypeVisitor<R,P> visitor,
                          final P param) {
        return visitor.visitDeclaredType(this, param);
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.DECLARED;
    }

    @Override
    public boolean isDeclaredType() {
        return true;
    }

    @Override
    public DeclaredType withTypeArgument(final Type<?> typeArgument) {
        return withTypeArguments(List.of(typeArgument));
    }

    @Override
    public DeclaredType withTypeArguments(final Type<?> first,
                                          final Type<?> second) {
        return withTypeArguments(List.of(first, second));
    }

    @Override
    public DeclaredType withTypeArguments(final List<? extends Type<?>> typeArguments) {
        final var element = getElement();
        return new JavaDeclaredType(
                element,
                new ArrayList<>(),
                typeArguments,
                isNullable
        );
    }

    @Override
    public DeclaredType withTypeArguments() {
        final var element = getElement();
        return new JavaDeclaredType(element, element.asType().isNullable());
    }

    @Override
    public boolean isAssignableBy(final Type<?> otherType) {
        if (!otherType.isDeclaredType()) {
            return false;
        }

        final var otherDeclaredType = (DeclaredType) otherType;

        if (!getElement().getQualifiedName()
                .equals(otherDeclaredType.getElement().getQualifiedName())) {
            return false;
        }

        final var typeArgsOptional = getTypeArguments();

        if (typeArgsOptional.isPresent()) {
            final var typeArgs = typeArgsOptional.get();
            final var otherTypeArgumentsOptional = otherDeclaredType.getTypeArguments();
            if (otherTypeArgumentsOptional.isEmpty()) {
                return false;
            }

            final var otherTypeArguments = otherTypeArgumentsOptional.get();

            for (int i = 0; i < typeArgs.size(); i++) {
                final var typeArgument = typeArgs.get(i);
                final var otherTypeArgument = otherTypeArguments.get(i);

                if (!typeArgument.isAssignableBy(otherTypeArgument)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean isSameType(final Type<?> otherType) {
        if (!otherType.isDeclaredType()) {
            return false;
        }

        final var otherDeclaredType = (DeclaredType) otherType;

        if (!getElement().getQualifiedName()
                .equals(otherDeclaredType.getElement().getQualifiedName())) {
            return false;
        }

        final var typeArgsOptional = getTypeArguments();

        if (typeArgsOptional.isPresent()) {
            final var typeArgs = typeArgsOptional.get();
            final var otherTypeArgs = otherDeclaredType.getTypeArguments();
            if (otherTypeArgs.isEmpty()) {
                return false;
            }

            final var otherTypeArguments = otherTypeArgs.get();

            for (int i = 0; i < typeArgs.size(); i++) {
                final var typeArgument = typeArgs.get(i);
                final var otherTypeArgument = otherTypeArguments.get(i);

                if (!typeArgument.isSameType(otherTypeArgument)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean equals(final @Nullable Object obj) {
        if (obj instanceof DeclaredType otherType) {
            return isSameType(otherType);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return getElement().getQualifiedName().hashCode();
    }

    @Override
    public boolean isNullable() {
        return isNullable;
    }

    @Override
    public DeclaredType asNullableType() {
        if (isNullable) {
            return this;
        } else {
            return new JavaDeclaredType(
                    getElement(),
                    this.getAnnotations(),
                    getTypeArguments().orElse(null),
                    true
            );
        }
    }

    @Override
    public Type<?> asNonNullableType() {
        return !isNullable ? this : new JavaDeclaredType(
                getElement(),
                getAnnotations(),
                getTypeArguments().orElse(null),
                false
        );
    }

    @Override
    public DeclaredType copy() {
        return new JavaDeclaredType(
                getElement(),
                getAnnotations(),
                getTypeArguments().orElse(null),
                isNullable
        );
    }

    @Override
    public Optional<Type<?>> getFirstTypeArg() {
        return getTypeArguments()
                .filter(it -> it.size() > 0)
                .map(it -> it.get(0));
    }
}
