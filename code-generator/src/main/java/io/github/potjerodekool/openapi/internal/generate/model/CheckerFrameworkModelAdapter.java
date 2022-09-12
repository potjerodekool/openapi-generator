package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.RequestCycleLocation;
import io.github.potjerodekool.openapi.generate.model.ModelAdapter;
import io.github.potjerodekool.openapi.internal.ast.element.MethodElement;
import io.github.potjerodekool.openapi.internal.ast.element.VariableElement;
import io.github.potjerodekool.openapi.internal.ast.expression.AnnotationExpression;
import io.github.potjerodekool.openapi.internal.ast.type.DeclaredType;
import io.github.potjerodekool.openapi.internal.di.Bean;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;

@Bean
public class CheckerFrameworkModelAdapter implements ModelAdapter {

    private boolean isEnabled;

    public CheckerFrameworkModelAdapter() {
    }

    public CheckerFrameworkModelAdapter(final OpenApiGeneratorConfig config) {
        this.isEnabled = config.isFeatureEnabled(OpenApiGeneratorConfig.FEATURE_CHECKER);
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

            declaredType.addAnnotation(
                    new AnnotationExpression("org.checkerframework.checker.nullness.qual.Nullable")
            );
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
                && !property.required()
                && httpMethod != HttpMethod.PATCH) {

            final var ct = (DeclaredType) returnType;
            ct.addAnnotation(new AnnotationExpression("org.checkerframework.checker.nullness.qual.Nullable"));
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

        final var propertyType = property.type();
        final var isNullable = propertyType.nullable();

        if (Boolean.TRUE.equals(isNullable)) {
            final var parameterType = method.getParameters().get(0).getType();
            parameterType.addAnnotation(new AnnotationExpression("org.checkerframework.checker.nullness.qual.Nullable"));
        }
    }
}
