package io.github.potjerodekool.openapi.internal.ast.expression;

import java.util.List;

public class ArrayInitializerExpression implements Expression {

    private final List<Expression> values;

    public ArrayInitializerExpression() {
        this(List.of());
    }

    public ArrayInitializerExpression(final Expression value) {
        this(List.of(value));
    }

    public ArrayInitializerExpression(final List<Expression> values) {
        this.values = values;
    }

    public List<Expression> getValues() {
        return values;
    }

    @Override
    public <R, P> R accept(final ExpressionVisitor<R, P> visitor, final P param) {
        return visitor.visitArrayInitializerExpression(this, param);
    }
}
