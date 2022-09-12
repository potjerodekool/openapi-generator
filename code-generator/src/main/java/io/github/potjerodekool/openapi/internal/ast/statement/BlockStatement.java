package io.github.potjerodekool.openapi.internal.ast.statement;

import io.github.potjerodekool.openapi.internal.ast.expression.Expression;

import java.util.ArrayList;
import java.util.List;

public class BlockStatement implements Statement {

    private final List<Statement> statements;

    public BlockStatement() {
        this(new ArrayList<>());
    }

    public BlockStatement(final Statement statement) {
        this(List.of(statement));
    }

    public BlockStatement(final Expression expression) {
        this(List.of(new ExpressionStatement(expression)));
    }

    public BlockStatement(final List<Statement> statements) {
        this.statements = statements;
    }

    public void add(final Statement statement) {
        this.statements.add(statement);
    }

    public void add(final Expression expression) {
        this.add(new ExpressionStatement(expression));
    }

    public List<Statement> getStatements() {
        return statements;
    }

    @Override
    public <R,P> R accept(final StatementVisitor<R, P> visitor,
                          final P param) {
        return visitor.visitBlockStatement(this, param);
    }
}
