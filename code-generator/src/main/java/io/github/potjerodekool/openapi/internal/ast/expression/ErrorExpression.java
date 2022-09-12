package io.github.potjerodekool.openapi.internal.ast.expression;

public class ErrorExpression implements Expression {

    @Override
    public <R, P> R accept(final ExpressionVisitor<R, P> visitor, final P param) {
        return visitor.visitErrorExpression(this, param);
    }
}
