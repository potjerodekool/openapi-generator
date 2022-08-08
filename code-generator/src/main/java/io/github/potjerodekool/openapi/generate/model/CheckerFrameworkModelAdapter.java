package io.github.potjerodekool.openapi.generate.model;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.expr.Name;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.RequestCycleLocation;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;

import static com.github.javaparser.ast.NodeList.nodeList;

public class CheckerFrameworkModelAdapter implements ModelAdapter {

    private final OpenApiGeneratorConfig config;

    public CheckerFrameworkModelAdapter(final OpenApiGeneratorConfig config) {
        this.config = config;
    }

    @Override
    public void adaptField(final HttpMethod httpMethod,
                           final RequestCycleLocation requestCycleLocation,
                           final OpenApiProperty property,
                           final FieldDeclaration fieldDeclaration) {
        final var isPatch = httpMethod == HttpMethod.PATCH;
        final var fieldType = fieldDeclaration.getVariables().get(0)
                .getType();

        if (!isPatch
                && fieldType.isClassOrInterfaceType()
                && config.isAddCheckerAnnotations()) {
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
        final var returnType = methodDeclaration.getType();

        if (!returnType.isPrimitiveType() &&
                !property.required() && config.isAddCheckerAnnotations() && httpMethod != HttpMethod.PATCH) {

            final var ct = (ClassOrInterfaceType) returnType;
            ct.addMarkerAnnotation("org.checkerframework.checker.nullness.qual.Nullable");
        }
    }

    @Override
    public void adaptSetter(final HttpMethod httpMethod,
                            final RequestCycleLocation requestCycleLocation,
                            final OpenApiProperty property,
                            final MethodDeclaration methodDeclaration) {
        final var propertyType = property.type();
        final var isNullable = propertyType.nullable();

        if (config.isAddCheckerAnnotations()) {
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
}
