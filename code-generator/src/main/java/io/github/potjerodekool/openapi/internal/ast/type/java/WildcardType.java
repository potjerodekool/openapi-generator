package io.github.potjerodekool.openapi.internal.ast.type.java;

import io.github.potjerodekool.openapi.internal.ast.element.AnnotationMirror;
import io.github.potjerodekool.openapi.internal.ast.element.Element;
import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import io.github.potjerodekool.openapi.internal.ast.type.TypeVisitor;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class WildcardType implements Type<Element> {

    private final Element element;
    private final @Nullable Type<TypeElement> extendsBound;
    private final @Nullable Type<TypeElement> superBound;
    private final boolean isNullable;

    public static WildcardType withExtendsBound(final Type<TypeElement> extendsBound) {
        return new WildcardType(extendsBound, null, false);
    }

    public static WildcardType withSuperBound(final Type<TypeElement> superBound) {
        return new WildcardType(null, superBound, false);
    }

    private WildcardType(final @Nullable Type<TypeElement> extendsBound,
                         final @Nullable Type<TypeElement> superBound,
                         final boolean isNullable) {
        this.element = TypeElement.create(ElementKind.OTHER, List.of(), Set.of(), "");
        this.extendsBound = extendsBound;
        this.superBound = superBound;
        this.isNullable = isNullable;
    }

    @Override
    public Element getElement() {
        return element;
    }

    @Override
    public Optional<List<? extends Type<?>>> getTypeArguments() {
        return Optional.empty();
    }

    @Override
    public List<AnnotationMirror> getAnnotations() {
        return List.of();
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

    @Override
    public Type<?> asNullableType() {
        return isNullable ? this : new WildcardType(
                extendsBound,
                superBound,
                true
        );
    }

    @Override
    public Type<?> asNonNullableType() {
        return !isNullable ? this : new WildcardType(
                extendsBound,
                superBound,
                false
        );
    }

    @Override
    public boolean isNullable() {
        return isNullable;
    }
}
