package io.github.potjerodekool.openapi.internal.ast.statement;

import io.github.potjerodekool.openapi.internal.ast.expression.Expression;

public class ExpressionStatement implements Statement {

    private final Expression expression;

    public ExpressionStatement(final Expression expression) {
        this.expression = expression;
    }

    public Expression getExpression() {
        return expression;
    }

    @Override
    public <R,P> R accept(final StatementVisitor<R, P> visitor,
                          final P param) {
        return visitor.visitExpressionStatement(this, param);
    }
}
