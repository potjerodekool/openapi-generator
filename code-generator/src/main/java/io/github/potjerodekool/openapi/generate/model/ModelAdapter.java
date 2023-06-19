package io.github.potjerodekool.openapi.generate.model;

import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.tree.MethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.ClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
import io.github.potjerodekool.codegen.model.type.TypeKind;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;

public interface ModelAdapter {

    default void adaptConstructor(MethodDeclaration constructor) {
    }

    default void adaptField(OpenApiProperty property,
                            VariableDeclaration field) {
    }

    default void adaptGetter(OpenApiProperty property,
                             MethodDeclaration method) {

    }

    default void adaptSetter(OpenApiProperty property,
                             MethodDeclaration method) {
    }

    default void adapt(final ClassDeclaration classDeclaration) {
        classDeclaration.getEnclosed().forEach(enclosed -> {
            if (enclosed instanceof MethodDeclaration methodDeclaration) {
                adaptMethodDeclaration(methodDeclaration);
            } else if (enclosed instanceof VariableDeclaration variableDeclaration) {
                //TODO
            }
        });
    }

    private void adaptMethodDeclaration(final MethodDeclaration methodDeclaration) {
        if (methodDeclaration.getKind() == ElementKind.CONSTRUCTOR) {
            adaptConstructor(methodDeclaration);
        } else if (methodDeclaration.getKind() == ElementKind.METHOD) {
            if (isGetter(methodDeclaration)) {
                adaptGetter(null, methodDeclaration);
            } else if (isSetter(methodDeclaration)) {
                adaptSetter(null, methodDeclaration);
            }
        }
    }

    private boolean isGetter(final MethodDeclaration methodDeclaration) {
        if (methodDeclaration.getParameters().size() > 0) {
            return false;
        }

        final var returnType = methodDeclaration.getReturnType().getType();
        return returnType.getKind() != TypeKind.VOID;
    }

    private boolean isSetter(final MethodDeclaration methodDeclaration) {
        return methodDeclaration.getParameters().size() == 1;
    }
}
