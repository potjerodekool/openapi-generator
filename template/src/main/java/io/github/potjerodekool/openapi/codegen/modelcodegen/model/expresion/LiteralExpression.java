package io.github.potjerodekool.openapi.codegen.modelcodegen.model.expresion;

public class LiteralExpression implements Expression {

    private final Object value;

    public LiteralExpression(final String value) {
        this.value = "\"" +  value + "\"";
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
