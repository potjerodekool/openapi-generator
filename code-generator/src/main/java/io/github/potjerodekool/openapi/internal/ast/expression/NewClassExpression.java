package io.github.potjerodekool.openapi.internal.ast.expression;

import io.github.potjerodekool.openapi.internal.ast.type.DeclaredType;

public class NewClassExpression implements Expression {

    private final DeclaredType classType;

    public NewClassExpression(final DeclaredType classType) {
        this.classType = classType;
    }

    public DeclaredType getClassType() {
        return classType;
    }

    @Override
    public <R, P> R accept(final ExpressionVisitor<R, P> visitor, final P param) {
        return visitor.visitNewClassExpression(this, param);
    }
}
