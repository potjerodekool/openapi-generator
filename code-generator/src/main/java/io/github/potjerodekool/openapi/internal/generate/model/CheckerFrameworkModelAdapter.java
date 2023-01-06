package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.openapi.*;
import io.github.potjerodekool.openapi.generate.model.ModelAdapter;
import io.github.potjerodekool.openapi.internal.ast.Attribute;
import io.github.potjerodekool.openapi.internal.ast.element.MethodElement;
import io.github.potjerodekool.openapi.internal.ast.element.VariableElement;
import io.github.potjerodekool.openapi.internal.ast.type.DeclaredType;
import io.github.potjerodekool.openapi.internal.ast.type.MutableType;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import io.github.potjerodekool.openapi.internal.di.Bean;
import io.github.potjerodekool.openapi.internal.di.ConditionalOnDependency;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;
import jakarta.inject.Inject;

@Bean
@ConditionalOnDependency(
        groupId = "org.checkerframework",
        artifactId = "checker-qual"
)
public class CheckerFrameworkModelAdapter implements ModelAdapter {

    private final boolean isEnabled;

    @Inject
    public CheckerFrameworkModelAdapter(final GeneratorConfig generatorConfig) {
        this.isEnabled = generatorConfig.language() == Language.JAVA
                && generatorConfig.isFeatureEnabled(Features.FEATURE_CHECKER);
    }

    @Override
    public void adaptConstructor(final HttpMethod httpMethod,
                                 final RequestCycleLocation requestCycleLocation,
                                 final MethodElement constructor) {
        if (!isEnabled) {
            return;
        }

        constructor.getParameters().forEach(parameter -> {
            final var parameterType = parameter.getType();
            visitType(parameterType);
        });
    }

    @Override
    public void adaptField(final HttpMethod httpMethod,
                           final RequestCycleLocation requestCycleLocation,
                           final OpenApiProperty property,
                           final VariableElement fieldElement) {
        if (!isEnabled) {
            return;
        }

        final var isPatch = httpMethod == HttpMethod.PATCH;
        final var fieldType = fieldElement.getType();

        if (!isPatch
                && fieldType.isDeclaredType()) {
            final var declaredType = (DeclaredType) fieldType;
            visitType(declaredType);
        }
    }

    @Override
    public void adaptGetter(final HttpMethod httpMethod,
                            final RequestCycleLocation requestCycleLocation,
                            final OpenApiProperty property,
                            final MethodElement methodDeclaration) {
        if (!isEnabled) {
            return;
        }

        final var returnType = methodDeclaration.getReturnType();

        if (!returnType.isPrimitiveType()
                && httpMethod != HttpMethod.PATCH) {
            visitType(returnType);
        }
    }

    @Override
    public void adaptSetter(final HttpMethod httpMethod,
                            final RequestCycleLocation requestCycleLocation,
                            final OpenApiProperty property,
                            final MethodElement method) {
        if (!isEnabled) {
            return;
        }

        visitType(method.getParameters().get(0).getType());
    }

    private void visitType(final Type<?> type) {
        if (type.isNullable() && type instanceof MutableType<?> mt) {
            mt.addAnnotation(Attribute.compound("org.checkerframework.checker.nullness.qual.Nullable"));
        }
    }
}
