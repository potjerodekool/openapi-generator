package io.github.potjerodekool.openapi.internal.ast.expression;

public class NameExpression implements Expression {

    private final String name;

    public NameExpression(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public <R, P> R accept(final ExpressionVisitor<R, P> visitor,
                              final P param) {
        return visitor.visitNameExpression(this, param);
    }
}
