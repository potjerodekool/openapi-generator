package io.github.potjerodekool.openapi.internal.ast.type;

import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;

public class WildcardType extends AbstractType<TypeElement> {

    private final @Nullable Type<TypeElement> extendsBound;
    private final @Nullable Type<TypeElement> superBound;

    public static WildcardType withExtendsBound(final Type<TypeElement> extendsBound) {
        return new WildcardType(extendsBound, null);
    }

    public static WildcardType withSuperBound(final Type<TypeElement> superBound) {
        return new WildcardType(null, superBound);
    }

    private WildcardType(final @Nullable Type<TypeElement> extendsBound,
                         final @Nullable Type<TypeElement> superBound) {
        super(TypeElement.WILDCARD);
        this.extendsBound = extendsBound;
        this.superBound = superBound;
    }

    public Optional<Type<TypeElement>> getExtendsBound() {
        return Optional.ofNullable(extendsBound);
    }

    public Optional<Type<TypeElement>> getSuperBound() {
        return Optional.ofNullable(superBound);
    }

    @Override
    public <R,P> R accept(final TypeVisitor<R,P> visitor,
                          final P param) {
        return visitor.visitWildcardType(this, param);
    }

    @Override
    public TypeKind getKind() {
        return TypeKind.WILDCARD;
    }

    @Override
    public boolean isAssignableBy(final Type<?> otherType) {
        if (!otherType.isWildCardType()) {
            return false;
        }

        final var otherWildcardType = (WildcardType) otherType;

        if (extendsBound != null) {
            return otherWildcardType.extendsBound != null
                    && extendsBound.isAssignableBy(otherWildcardType.extendsBound);
        } else if (superBound != null) {
            return otherWildcardType.superBound != null
                    && superBound.isAssignableBy(otherWildcardType.superBound);
        }

        return false;
    }

    @Override
    public boolean isSameType(final Type<?> otherType) {
        if (!otherType.isWildCardType()) {
            return false;
        }

        final var otherWildcardType = (WildcardType) otherType;

        if (extendsBound != null) {
            return otherWildcardType.extendsBound != null
                    && extendsBound.isSameType(otherWildcardType.extendsBound);
        } else if (superBound != null) {
            return otherWildcardType.superBound != null
                    && superBound.isSameType(otherWildcardType.superBound);
        }

        return false;
    }

    @Override
    public boolean isWildCardType() {
        return true;
    }
}
