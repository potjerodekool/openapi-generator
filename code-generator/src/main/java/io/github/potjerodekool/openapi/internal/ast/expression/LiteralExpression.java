package io.github.potjerodekool.openapi.internal.ast.expression;

import io.github.potjerodekool.openapi.internal.ast.LiteralType;
import io.github.potjerodekool.openapi.internal.ast.type.Type;

public interface LiteralExpression extends Expression {

    static LiteralExpression createNullLiteralExpression() {
        return StringValueLiteralExpression.NULL;
    }

    static LiteralExpression createClassLiteralExpression(final Type<?> type) {
        return new ClassLiteralExpression(type);
    }

    static LiteralExpression createBooleanLiteralExpression() {
        return createBooleanLiteralExpression(false);
    }

    static LiteralExpression createBooleanLiteralExpression(final boolean value) {
        return value
                ? StringValueLiteralExpression.TRUE
                : StringValueLiteralExpression.FALSE;
    }

    static LiteralExpression createCharLiteralExpression() {
        return new StringValueLiteralExpression("?", LiteralType.CHAR);
    }

    static LiteralExpression createCharLiteralExpression(final char value) {
        return new StringValueLiteralExpression(Character.toString(value), LiteralType.CHAR);
    }

    static LiteralExpression createByteLiteralExpression(final byte value) {
        return new StringValueLiteralExpression(Byte.toString(value), LiteralType.BYTE);
    }

    static LiteralExpression createShortLiteralExpression(final short value) {
        return new StringValueLiteralExpression(Short.toString(value), LiteralType.SHORT);
    }

    static LiteralExpression createIntLiteralExpression() {
        return new StringValueLiteralExpression("0", LiteralType.INT);
    }

    static LiteralExpression createIntLiteralExpression(final String value) {
        return new StringValueLiteralExpression(value, LiteralType.INT);
    }

    static LiteralExpression createLongLiteralExpression() {
        return new StringValueLiteralExpression("0", LiteralType.LONG);
    }

    static LiteralExpression createLongLiteralExpression(final String value) {
        return new StringValueLiteralExpression(value, LiteralType.LONG);
    }

    static LiteralExpression createFloatLiteralExpression() {
        return new StringValueLiteralExpression("0", LiteralType.FLOAT);
    }

    static LiteralExpression createFloatLiteralExpression(final float value) {
        return new StringValueLiteralExpression(Float.toString(value), LiteralType.FLOAT);
    }

    static LiteralExpression createDoubleLiteralExpression() {
        return new StringValueLiteralExpression("0", LiteralType.DOUBLE);
    }

    static LiteralExpression createDoubleLiteralExpression(final double value) {
        return new StringValueLiteralExpression(Double.toString(value), LiteralType.DOUBLE);
    }

    static LiteralExpression createStringLiteralExpression(final String value) {
        return new StringValueLiteralExpression(value, LiteralType.STRING);
    }

    LiteralType getLiteralType();
}
