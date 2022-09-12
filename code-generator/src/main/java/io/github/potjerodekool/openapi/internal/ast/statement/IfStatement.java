package io.github.potjerodekool.openapi.internal.ast.statement;

import io.github.potjerodekool.openapi.internal.ast.expression.Expression;

public class IfStatement implements Statement {

    private final Expression condition;

    private final BlockStatement body;

    public IfStatement(final Expression condition,
                       final BlockStatement body) {
        this.condition = condition;
        this.body = body;
    }

    public Expression getCondition() {
        return condition;
    }

    public BlockStatement getBody() {
        return body;
    }

    @Override
    public <R, P> R accept(final StatementVisitor<R, P> visitor, final P param) {
        return visitor.visitIfStatement(this, param);
    }
}
