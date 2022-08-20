package io.github.potjerodekool.openapi.internal.generate.model;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.RequestCycleLocation;
import io.github.potjerodekool.openapi.generate.model.ModelAdapter;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;

import static com.github.javaparser.ast.NodeList.nodeList;

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
                           final FieldDeclaration fieldDeclaration) {
        if (!isEnabled) {
            return;
        }

        final var isPatch = httpMethod == HttpMethod.PATCH;
        final var fieldType = fieldDeclaration.getVariables().get(0)
                .getType();

        if (!isPatch
                && fieldType.isClassOrInterfaceType()) {
            fieldType.setAnnotations(
                    nodeList(new MarkerAnnotationExpr(new Name("org.checkerframework.checker.nullness.qual.Nullable")))
            );
        }
    }

    @Override
    public void adaptGetter(final HttpMethod httpMethod,
                            final RequestCycleLocation requestCycleLocation,
                            final OpenApiProperty property,
                            final MethodDeclaration methodDeclaration) {
        if (!isEnabled) {
            return;
        }

        final var returnType = methodDeclaration.getType();

        if (!returnType.isPrimitiveType()
                && !property.required()
                && httpMethod != HttpMethod.PATCH) {

            final var ct = (ClassOrInterfaceType) returnType;
            ct.addMarkerAnnotation("org.checkerframework.checker.nullness.qual.Nullable");
        }
    }

    @Override
    public void adaptSetter(final HttpMethod httpMethod,
                            final RequestCycleLocation requestCycleLocation,
                            final OpenApiProperty property,
                            final MethodDeclaration methodDeclaration) {
        if (!isEnabled) {
            return;
        }

        final var propertyType = property.type();
        final var isNullable = propertyType.nullable();

        if (Boolean.TRUE.equals(isNullable)) {
            final var parameterType = methodDeclaration.getParameter(0).getType();

            parameterType.setAnnotations(
                    nodeList(
                            new MarkerAnnotationExpr(new Name("org.checkerframework.checker.nullness.qual.Nullable"))
                    )
            );
        }
    }
}
