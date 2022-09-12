package io.github.potjerodekool.openapi.internal.ast.statement;

public class ErrorStatement implements Statement {

    @Override
    public <R, P> R accept(final StatementVisitor<R, P> visitor, final P param) {
        return visitor.visitErrorStatement(this, param);
    }
}
