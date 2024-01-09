package io.github.potjerodekool.openapi.internal.generate.model.adapt;

import io.github.potjerodekool.codegen.model.CompilationUnit;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.MethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.expression.Expression;
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.AnnotatedTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.ArrayTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.openapi.internal.ClassNames;
import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.media.ObjectSchema;

import java.util.List;
import java.util.Set;

public class CheckerModelAdapter implements ModelAdapter {

    private static final Set<String> CONTAINER_CLASSES = Set.of(
            "org.openapitools.jackson.nullable.JsonNullable",
            ClassNames.LIST_CLASS_NAME,
            "java.util.Set"
    );


    @Override
    public void adapt(final HttpMethod method,
                      final ObjectSchema schema,
                      final CompilationUnit unit) {
        final var classDeclarationOptional = unit.getClassDeclarations().stream()
                .findFirst();

        classDeclarationOptional.ifPresent(classDeclaration ->
                classDeclaration.getEnclosed().forEach(enclosed -> {
                if (enclosed instanceof VariableDeclaration variableDeclaration) {
                    adaptVariableIfNeeded(variableDeclaration);
                } else if (enclosed instanceof MethodDeclaration methodDeclaration) {
                    adaptMethodIfNeeded(methodDeclaration);
                }
            }
        ));
    }

    private void adaptVariableIfNeeded(final VariableDeclaration variableDeclaration) {
        final var newType = addAnnotationIfNeeded(variableDeclaration.getVarType());
        variableDeclaration.varType(newType);
    }

    private void adaptMethodIfNeeded(final MethodDeclaration methodDeclaration) {
        final var returnType = addAnnotationIfNeeded(methodDeclaration.getReturnType());
        methodDeclaration.returnType(returnType);
        methodDeclaration.getParameters().forEach(this::adaptVariableIfNeeded);
    }

    private Expression addAnnotationIfNeeded(final Expression type) {
        if (!isContainerType(type) && isNullable(type) && type instanceof ClassOrInterfaceTypeExpression classOrInterfaceTypeExpression) {
            return new AnnotatedTypeExpression(
                    classOrInterfaceTypeExpression,
                    List.of(new AnnotationExpression("org.checkerframework.checker.nullness.qual.Nullable"))
            );
        } else {
            return type;
        }
    }

    private boolean isNullable(final Expression type) {
        return type instanceof ClassOrInterfaceTypeExpression classOrInterfaceTypeExpression
                && classOrInterfaceTypeExpression.isNullable();
    }

    private boolean isContainerType(final Expression type) {
        if (type instanceof ArrayTypeExpression) {
            return true;
        }

        if (type instanceof ClassOrInterfaceTypeExpression classOrInterfaceTypeExpression) {
            final var className = classOrInterfaceTypeExpression.getName().toString();
            return CONTAINER_CLASSES.contains(className);
        }

        return false;
    }
}
