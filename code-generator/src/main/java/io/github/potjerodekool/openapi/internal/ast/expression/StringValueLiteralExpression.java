package io.github.potjerodekool.openapi.internal.ast.expression;

import io.github.potjerodekool.openapi.internal.ast.LiteralType;

public class StringValueLiteralExpression implements LiteralExpression {

    static final LiteralExpression NULL = new StringValueLiteralExpression("null", LiteralType.NULL);

    static final LiteralExpression TRUE = new StringValueLiteralExpression("true", LiteralType.BOOLEAN);
    static final LiteralExpression FALSE = new StringValueLiteralExpression("false", LiteralType.BOOLEAN);

    private final String value;

    private final LiteralType literalType;

    StringValueLiteralExpression(final String value, LiteralType literalType) {
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
