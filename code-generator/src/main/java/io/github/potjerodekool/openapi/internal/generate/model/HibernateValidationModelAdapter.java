package io.github.potjerodekool.openapi.internal.generate.model;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import io.github.potjerodekool.openapi.DependencyChecker;
import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.RequestCycleLocation;
import io.github.potjerodekool.openapi.internal.generate.GenerateUtils;
import io.github.potjerodekool.openapi.internal.generate.Types;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;
import org.checkerframework.checker.nullness.qual.EnsuresNonNull;

@SuppressWarnings("initialization.field.uninitialized")
public class HibernateValidationModelAdapter implements InternalModelAdapter {

    private Types types;

    private boolean enabled = false;

    @Override
    @EnsuresNonNull("this.types")
    public void init(final OpenApiGeneratorConfig config,
                     final Types types,
                     final DependencyChecker dependencyChecker,
                     final GenerateUtils generateUtils) {
        this.types = types;
        var enabled = config.getFeatureValue(OpenApiGeneratorConfig.FEATURE_HIBERNATE_VALIDATION);

        if (enabled == null) {
            enabled = dependencyChecker.isDependencyPresent("org.hibernate.validator", "hibernate-validator");
        }

        this.enabled = Boolean.TRUE.equals(enabled);
    }

    @Override
    public void adaptField(final HttpMethod httpMethod,
                           final RequestCycleLocation requestCycleLocation,
                           final OpenApiProperty property,
                           final FieldDeclaration fieldDeclaration) {
        if (!enabled) {
            return;
        }

        processUniqueItems(property, fieldDeclaration);
   }

    private void processUniqueItems(final OpenApiProperty property,
                                    final FieldDeclaration fieldDeclaration) {
        final var fieldType = fieldDeclaration.getVariable(0).getType();

        if (Boolean.TRUE.equals(property.uniqueItems()) && types.isListType(fieldType)) {
            fieldDeclaration.addAnnotation(
                    new MarkerAnnotationExpr("org.hibernate.validator.constraints.UniqueElements")
            );
        }
    }

}
