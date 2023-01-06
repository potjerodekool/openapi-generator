package io.github.potjerodekool.openapi.internal.ast.type.java;

import io.github.potjerodekool.openapi.internal.ast.element.AnnotationMirror;
import io.github.potjerodekool.openapi.internal.ast.element.Element;
import io.github.potjerodekool.openapi.internal.ast.type.PrimitiveType;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import io.github.potjerodekool.openapi.internal.ast.type.TypeVisitor;

import javax.lang.model.type.TypeKind;
import java.util.List;
import java.util.Optional;

public class JavaPrimitiveType implements Type<Element>, PrimitiveType {

    private final TypeKind kind;
    private final Element element;

    public JavaPrimitiveType(final TypeKind kind,
                             final Element element) {
        this.kind = kind;
        this.element = element;
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
    public <R,P> R accept(final TypeVisitor<R,P> visitor,
                          final P param) {
        return switch (kind) {
            case BOOLEAN -> visitor.visitBooleanType(this, param);
            case BYTE -> visitor.visitByteType(this, param);
            case SHORT -> visitor.visitShortType(this, param);
            case INT -> visitor.visitIntType(this, param);
            case LONG -> visitor.visitLongType(this, param);
            case CHAR -> visitor.visitCharType(this, param);
            case FLOAT -> visitor.visitFloatType(this, param);
            case DOUBLE -> visitor.visitDoubleType(this, param);
            default -> visitor.visitUnknownType(this, param);
        };
    }

    @Override
    public TypeKind getKind() {
        return kind;
    }

    @Override
    public boolean isPrimitiveType() {
        return true;
    }

    @Override
    public boolean isAssignableBy(final Type<?> otherType) {
        if (isSameType(otherType)) {
            return true;
        }
        //TODO check declared type
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isSameType(final Type<?> otherType) {
        return kind == otherType.getKind();
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
