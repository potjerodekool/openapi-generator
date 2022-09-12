package io.github.potjerodekool.openapi.internal.ast.expression;

import io.github.potjerodekool.openapi.internal.ast.LiteralType;
import io.github.potjerodekool.openapi.internal.ast.type.JavaArrayType;
import io.github.potjerodekool.openapi.internal.ast.type.DeclaredType;
import io.github.potjerodekool.openapi.internal.ast.type.Type;

public class LiteralExpression implements Expression {

    private static final LiteralExpression NULL = new LiteralExpression("null", LiteralType.NULL);

    private static final LiteralExpression TRUE = new LiteralExpression("true", LiteralType.BOOLEAN);
    private static final LiteralExpression FALSE = new LiteralExpression("false", LiteralType.BOOLEAN);

    public static LiteralExpression createNullLiteralExpression() {
        return NULL;
    }

    public static LiteralExpression createClassLiteralExpression(final Class<?> clazz) {
        return new LiteralExpression(clazz.getName(), LiteralType.CLASS);
    }

    public static LiteralExpression createClassLiteralExpression(final Type<?> type) {
        final var name = getTypeName(type);
        return new LiteralExpression(name, LiteralType.CLASS);
    }

    private static String getTypeName(final Type<?> type) {
        return switch (type.getKind()) {
            case BOOLEAN -> "boolean";
            case BYTE -> "byte";
            case SHORT -> "short";
            case INT -> "int";
            case LONG -> "long";
            case CHAR -> "char";
            case FLOAT -> "float";
            case DOUBLE -> "double";
            case VOID -> "void";
            case ARRAY -> {
                final var arrayType = (JavaArrayType) type;
                yield getTypeName(arrayType.getComponentType()) + "[]";
            }
            case DECLARED -> {
                final var declaredType = (DeclaredType) type;
                yield declaredType.getElement().getQualifiedName();
            }
            default -> throw new UnsupportedOperationException(type.getKind() + "");
        };
    }

    public static LiteralExpression createClassLiteralExpression(final String className) {
        return new LiteralExpression(className, LiteralType.CLASS);
    }

    public static LiteralExpression createBooleanLiteralExpression() {
        return createBooleanLiteralExpression(false);
    }

    public static LiteralExpression createBooleanLiteralExpression(final boolean value) {
        return value
                ? TRUE
                : FALSE;
    }

    public static LiteralExpression createCharLiteralExpression() {
        return new LiteralExpression("?", LiteralType.CHAR);
    }

    public static LiteralExpression createCharLiteralExpression(final char value) {
        return new LiteralExpression(Character.toString(value), LiteralType.CHAR);
    }

    public static LiteralExpression createByteLiteralExpression(final byte value) {
        return new LiteralExpression(Byte.toString(value), LiteralType.BYTE);
    }

    public static LiteralExpression createShortLiteralExpression(final short value) {
        return new LiteralExpression(Short.toString(value), LiteralType.SHORT);
    }

    public static LiteralExpression createIntLiteralExpression() {
        return new LiteralExpression("0", LiteralType.INT);
    }

    public static LiteralExpression createIntLiteralExpression(final String value) {
        return new LiteralExpression(value, LiteralType.INT);
    }

    public static LiteralExpression createLongLiteralExpression() {
        return new LiteralExpression("0", LiteralType.LONG);
    }

    public static LiteralExpression createLongLiteralExpression(final String value) {
        return new LiteralExpression(value, LiteralType.LONG);
    }

    public static LiteralExpression createFloatLiteralExpression() {
        return new LiteralExpression("0", LiteralType.FLOAT);
    }

    public static LiteralExpression createFloatLiteralExpression(final float value) {
        return new LiteralExpression(Float.toString(value), LiteralType.FLOAT);
    }

    public static LiteralExpression createDoubleLiteralExpression() {
        return new LiteralExpression("0", LiteralType.DOUBLE);
    }

    public static LiteralExpression createDoubleLiteralExpression(final double value) {
        return new LiteralExpression(Double.toString(value), LiteralType.DOUBLE);
    }

    public static LiteralExpression createStringLiteralExpression(final String value) {
        return new LiteralExpression(value, LiteralType.STRING);
    }

    private final String value;

    private final LiteralType literalType;

    private LiteralExpression(final String value, LiteralType literalType) {
        this.value = value;
        this.literalType = literalType;
    }

    public LiteralType getLiteralType() {
        return literalType;
    }

    public String getValue() {
        return value;
    }

    @Override
    public <R, P> R accept(final ExpressionVisitor<R, P> visitor,
                           final P param) {
        return visitor.visitLiteralExpression(this, param);
    }
}
