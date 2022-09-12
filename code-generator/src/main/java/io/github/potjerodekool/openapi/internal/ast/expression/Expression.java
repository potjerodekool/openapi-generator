package io.github.potjerodekool.openapi.internal.ast.expression;

import io.github.potjerodekool.openapi.internal.ast.AstNode;

public interface Expression extends AstNode {

    <R, P> R accept(ExpressionVisitor<R, P> visitor,
                    P param);
}
