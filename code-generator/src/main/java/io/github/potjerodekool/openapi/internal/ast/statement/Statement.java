package io.github.potjerodekool.openapi.internal.ast.statement;

import io.github.potjerodekool.openapi.internal.ast.AstNode;

public interface Statement extends AstNode {

    <R, P> R accept(StatementVisitor<R, P> visitor, P param);
}
