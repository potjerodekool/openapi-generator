package io.github.potjerodekool.openapi.common.generate.model.expresion;

public class ClassOrInterfaceTypeExpression implements Expression {

    private String name;

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.CLASS_TYPE;
    }

    public String getName() {
        return name;
    }

    public ClassOrInterfaceTypeExpression name(final String name) {
        this.name = name;
        return this;
    }
}
