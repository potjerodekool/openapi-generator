package io.github.potjerodekool.openapi.internal.ast.type;

import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;

public class PrimitiveType extends AbstractType<TypeElement> {

    public static final PrimitiveType BOOLEAN = new PrimitiveType(TypeKind.BOOLEAN);
    public static final PrimitiveType BYTE = new PrimitiveType(TypeKind.BYTE);
    public static final PrimitiveType SHORT = new PrimitiveType(TypeKind.SHORT);
    public static final PrimitiveType INT = new PrimitiveType(TypeKind.INT);
    public static final PrimitiveType LONG = new PrimitiveType(TypeKind.LONG);
    public static final PrimitiveType CHAR = new PrimitiveType(TypeKind.CHAR);
    public static final PrimitiveType FLOAT = new PrimitiveType(TypeKind.FLOAT);
    public static final PrimitiveType DOUBLE = new PrimitiveType(TypeKind.DOUBLE);

    private final TypeKind kind;

    private PrimitiveType(final TypeKind kind) {
        super(TypeElement.PRIMITIVE);
        this.kind = kind;
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
        if (otherType.isPrimitiveType()) {
            return otherType == this;
        }
        return false;
    }
}
