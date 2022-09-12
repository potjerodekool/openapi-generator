package io.github.potjerodekool.openapi.internal.ast.statement;


public interface StatementVisitor<R, P> {

    R visitUnknown(Statement statement,
                   P param);

    R visitBlockStatement(BlockStatement blockStatement,
                          P param);

    R visitExpressionStatement(ExpressionStatement expressionStatement,
                               P param);

    R visitReturnStatement(ReturnStatement returnStatement,
                           P param);

    R visitIfStatement(IfStatement ifStatement,
                       P param);

    default R visitErrorStatement(final ErrorStatement errorStatement,
                                  final P param) {
        return visitUnknown(errorStatement, param);
    }
}
