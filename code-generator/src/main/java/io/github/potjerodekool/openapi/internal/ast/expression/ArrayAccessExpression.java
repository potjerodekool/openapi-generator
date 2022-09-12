package io.github.potjerodekool.openapi.internal.ast.expression;

public class ArrayAccessExpression implements Expression {

    private final Expression arrayExpression;

    private final Expression indexExpression;

    public ArrayAccessExpression(final Expression arrayExpression,
                                 final Expression indexExpression) {
        this.arrayExpression = arrayExpression;
        this.indexExpression = indexExpression;
    }

    public Expression getArrayExpression() {
        return arrayExpression;
    }

    public Expression getIndexExpression() {
        return indexExpression;
    }

    @Override
    public <R, P> R accept(final ExpressionVisitor<R, P> visitor, final P param) {
        return visitor.visitArrayAccessExpression(this, param);
    }
}
