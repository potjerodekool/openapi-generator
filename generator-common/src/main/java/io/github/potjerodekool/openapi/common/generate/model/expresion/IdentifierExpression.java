package io.github.potjerodekool.openapi.common.generate.model.expresion;

public class IdentifierExpression implements Expression {

    private String name;

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.IDENTIFIER;
    }

    public String getName() {
        return name;
    }

    public IdentifierExpression name(final String name) {
        this.name = name;
        return this;
    }
}
