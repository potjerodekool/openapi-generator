package io.github.potjerodekool.openapi.internal.ast.type;

import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;
import io.github.potjerodekool.openapi.internal.ast.expression.AnnotationExpression;
import io.github.potjerodekool.openapi.internal.util.Utils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Optional;

public class DeclaredType extends AbstractType<TypeElement> {

    private final boolean isNullable;

    private @Nullable List<? extends Type<?>> typeArguments;

    public DeclaredType(final TypeElement typeElement,
                        final boolean isNullable) {
        super(typeElement);
        this.isNullable = isNullable;
    }

    public DeclaredType(final TypeElement typeElement,
                        final List<AnnotationExpression> annotations,
                        final boolean isNullable) {
        super(typeElement, annotations);
        this.isNullable = isNullable;
    }

    public static DeclaredType create(final TypeElement typeElement,
                                      final List<AnnotationExpression> annotations,
                                      final @Nullable List<? extends Type<?>> typeArguments,
                                      final boolean isNullable) {
        final var declaredType = new DeclaredType(
                typeElement,
                annotations,
                isNullable
        );
        declaredType.typeArguments = typeArguments;
        return declaredType;
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

    public Optional<List<? extends Type<?>>> getTypeArguments() {
        return Optional.ofNullable(typeArguments);
    }

    public DeclaredType withTypeArgument(final @Nullable Type<?> typeArgument) {
        if (typeArgument == null) {
            return withTypeArguments();
        } else {
            return withTypeArguments(List.of(typeArgument));
        }
    }

    public DeclaredType withTypeArguments(final DeclaredType first,
                                          final DeclaredType second) {
        return withTypeArguments(List.of(first, second));
    }

    public DeclaredType withTypeArguments(final List<? extends Type<?>> typeArguments) {
        final var element = getElement();
        final var type = new DeclaredType(element, element.asType().isNullable);
        type.typeArguments = typeArguments;
        return type;
    }

    public DeclaredType withTypeArguments() {
        final var element = getElement();
        return new DeclaredType(element, element.asType().isNullable);
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

        if (hasTypeArguments()) {
            final var typeArgs = Utils.requireNonNull(typeArguments);
            final var otherTypeArguments = Utils.requireNonNull(otherDeclaredType.typeArguments);

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

        if (hasTypeArguments()) {
            final var typeArgs = Utils.requireNonNull(typeArguments);
            final var otherTypeArguments = Utils.requireNonNull(otherDeclaredType.typeArguments);

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

    private boolean hasTypeArguments() {
        return typeArguments != null;
    }

    @Override
    public boolean isNullable() {
        return isNullable;
    }

    public DeclaredType asNullableType() {
        if (isNullable) {
            return this;
        } else {
            return new DeclaredType(
                    getElement(),
                    this.getAnnotations(),
                    true);
        }
    }

}
