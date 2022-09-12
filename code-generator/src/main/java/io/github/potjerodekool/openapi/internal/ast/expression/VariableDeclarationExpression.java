package io.github.potjerodekool.openapi.internal.ast.expression;

import io.github.potjerodekool.openapi.internal.ast.Modifier;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Optional;
import java.util.Set;

public class VariableDeclarationExpression implements Expression {

    private final Set<Modifier> modifiers;

    private final Type<?> type;

    private final String name;

    private final @Nullable Expression initExpression;

    public VariableDeclarationExpression(final Set<Modifier> modifiers,
                                         final Type<?> type,
                                         final String name,
                                         final @Nullable Expression initExpression) {
        this.modifiers = modifiers;
        this.type = type;
        this.name = name;
        this.initExpression = initExpression;
    }

    public Set<Modifier> getModifiers() {
        return modifiers;
    }

    public Type<?> getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Optional<Expression> getInitExpression() {
        return Optional.ofNullable(initExpression);
    }

    @Override
    public <R, P> R accept(final ExpressionVisitor<R, P> visitor,
                           final P param) {
        return visitor.visitVariableDeclarationExpression(this, param);
    }
}
