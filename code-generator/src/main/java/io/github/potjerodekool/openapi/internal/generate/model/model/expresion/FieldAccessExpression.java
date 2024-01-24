package io.github.potjerodekool.openapi.internal.generate.model.model.expresion;

public class FieldAccessExpression implements Expression {

    private Expression target;

    private Expression field;

    @Override
    public ExpressionKind getKind() {
        return ExpressionKind.FIELD_ACCESS;
    }

    public Expression getTarget() {
        return target;
    }

    public FieldAccessExpression target(final Expression target) {
        this.target = target;
        return this;
    }

    public Expression getField() {
        return field;
    }

    public FieldAccessExpression field(final Expression field) {
        this.field = field;
        return this;
    }
}
