package io.github.potjerodekool.openapi.common.generate.model.expresion;

public class LiteralExpression implements Expression {

    private final Object value;

    public LiteralExpression(final String value) {
        this.value = "\"" +  value + "\"";
    }

    public LiteralExpression(final long value) {
        this.value = value + "L";
    }

    public LiteralExpression(final int value) {
        this.value = Integer.toString(value);
    }

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.LITERAL;
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
