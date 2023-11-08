package io.github.potjerodekool.openapi.generate.model;

import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.tree.MethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.Tree;
import io.github.potjerodekool.codegen.model.tree.java.JMethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.java.JVariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.NoTypeExpression;

public interface ModelAdapter {

    default void adaptConstructor(MethodDeclaration<?> constructor) {
    }

    default void adaptField(VariableDeclaration<?> field) {
    }

    default void adaptGetter(MethodDeclaration<?> method) {
    }

    default void adaptSetter(MethodDeclaration<?> method) {
    }

    default void adapt(final Tree tree) {
        if (tree instanceof JMethodDeclaration methodDeclaration) {
            if (methodDeclaration.getKind() == ElementKind.CONSTRUCTOR) {
                adaptConstructor(methodDeclaration);
            } else if (isGetter(methodDeclaration)) {
                adaptGetter(methodDeclaration);
            } else if (isSetter(methodDeclaration)) {
                adaptSetter(methodDeclaration);
            }
        } else if (tree instanceof JVariableDeclaration variableDeclaration
            && variableDeclaration.getKind() == ElementKind.FIELD) {
            adaptField(variableDeclaration);
        }
    }

    private boolean isGetter(final JMethodDeclaration methodDeclaration) {
        return methodDeclaration.getParameters().isEmpty()
                || !(methodDeclaration.getReturnType() instanceof NoTypeExpression);
    }

    private boolean isSetter(final JMethodDeclaration methodDeclaration) {
        return methodDeclaration.getParameters().size() == 1;
    }
}
