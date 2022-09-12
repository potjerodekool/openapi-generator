package io.github.potjerodekool.openapi.internal.ast.expression;

public class NamedMethodArgumentExpression implements Expression {

    private final String name;

    private final Expression argument;

    public NamedMethodArgumentExpression(final String name,
                                         final Expression argument) {
        this.name = name;
        this.argument = argument;
    }

    public String getName() {
        return name;
    }

    public Expression getArgument() {
        return argument;
    }

    @Override
    public <R, P> R accept(final ExpressionVisitor<R, P> visitor, final P param) {
        return visitor.visitNamedMethodArgumentExpression(this, param);
    }
}
