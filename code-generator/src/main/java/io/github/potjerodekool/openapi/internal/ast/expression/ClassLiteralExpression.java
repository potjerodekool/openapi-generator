package io.github.potjerodekool.openapi.internal.ast.expression;

import io.github.potjerodekool.openapi.internal.ast.LiteralType;
import io.github.potjerodekool.openapi.internal.ast.type.Type;

public class ClassLiteralExpression implements LiteralExpression {

    private final Type<?> type;

    public ClassLiteralExpression(final Type<?> type) {
        this.type = type;
    }

    public Type<?> getType() {
        return type;
    }

    @Override
    public <R, P> R accept(final ExpressionVisitor<R, P> visitor, final P param) {
        return visitor.visitLiteralExpression(this, param);
    }

    @Override
    public LiteralType getLiteralType() {
        return LiteralType.CLASS;
    }
}
