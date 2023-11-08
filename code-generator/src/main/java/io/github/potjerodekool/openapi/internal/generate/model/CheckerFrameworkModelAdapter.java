package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.MethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.expression.Expression;
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.AnnotatedTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.ArrayTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.openapi.Features;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.generate.model.ModelAdapter;
import io.github.potjerodekool.openapi.internal.ClassNames;
import io.github.potjerodekool.openapi.internal.di.Bean;
import io.github.potjerodekool.openapi.internal.di.ConditionalOnDependency;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Set;

@Bean
@ConditionalOnDependency(
        groupId = "org.checkerframework",
        artifactId = "checker-qual"
)
public class CheckerFrameworkModelAdapter implements ModelAdapter {

    private static final Set<String> CONTAINER_CLASSES = Set.of(
            "org.openapitools.jackson.nullable.JsonNullable",
            ClassNames.LIST_CLASS_NAME,
            "java.util.Set"
    );

    private final boolean isEnabled;

    @Inject
    public CheckerFrameworkModelAdapter(final GeneratorConfig generatorConfig) {
        this.isEnabled = generatorConfig.language() == Language.JAVA
                && generatorConfig.isFeatureEnabled(Features.FEATURE_CHECKER);
    }

    @Override
    public void adaptConstructor(final MethodDeclaration<?> constructor) {
        if (!isEnabled) {
            return;
        }

        constructor.getParameters().forEach(parameter -> {
            final var newType = addAnnotationIfNeeded(parameter.getVarType());
            parameter.varType(newType);
        });
    }

    @Override
    public void adaptField(final VariableDeclaration<?> fieldDeclaration) {
        if (!isEnabled) {
            return;
        }

        final var newFieldType = addAnnotationIfNeeded(fieldDeclaration.getVarType());
        fieldDeclaration.varType(newFieldType);
    }

    @Override
    public void adaptGetter(final MethodDeclaration<?> methodDeclaration) {
        if (!isEnabled) {
            return;
        }

        final var returnType = addAnnotationIfNeeded(methodDeclaration.getReturnType());
        methodDeclaration.setReturnType(returnType);
    }

    @Override
    public void adaptSetter(final MethodDeclaration<?> method) {
        if (!isEnabled) {
            return;
        }

        final var parameter = method.getParameters().get(0);
        final var newParameterType = addAnnotationIfNeeded(parameter.getVarType());
        parameter.varType(newParameterType);
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
