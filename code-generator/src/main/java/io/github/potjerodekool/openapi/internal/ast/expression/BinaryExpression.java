package io.github.potjerodekool.openapi.internal.ast.expression;

import io.github.potjerodekool.openapi.internal.ast.Operator;

public class BinaryExpression implements Expression {

    private final Expression left;

    private final Expression right;

    private final Operator operator;

    public BinaryExpression(final Expression left,
                            final Expression right,
                            final Operator operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }

    public Expression getLeft() {
        return left;
    }

    public Expression getRight() {
        return right;
    }

    public Operator getOperator() {
        return operator;
    }

    @Override
    public <R, P> R accept(final ExpressionVisitor<R, P> visitor,
                           final P param) {
        return visitor.visitBinaryExpression(this, param);
    }
}
