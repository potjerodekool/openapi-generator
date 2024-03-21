package io.github.potjerodekool.openapi.common.generate.model.adapter;

import io.github.potjerodekool.codegen.template.model.annotation.Annot;
import io.github.potjerodekool.codegen.template.model.annotation.AnnotTarget;
import io.github.potjerodekool.codegen.template.model.type.ClassOrInterfaceTypeExpr;
import io.github.potjerodekool.openapi.common.dependency.Bean;
import io.github.potjerodekool.openapi.common.dependency.ConditionalOnDependency;
import io.github.potjerodekool.openapi.common.generate.model.element.Model;
import io.swagger.v3.oas.models.media.ObjectSchema;

@Bean
@ConditionalOnDependency(groupId = "org.checkerframework", artifactId = "checker-qual")
public class CheckerModelAdapter implements ModelAdapter {
    @Override
    public void adapt(final Model model, final ObjectSchema schema) {
        adaptProperties(model, schema);
    }

    private void adaptProperties(final Model model,
                                 final ObjectSchema schema) {
        model.getProperties().forEach(modelProperty -> {
            final var name = modelProperty.getSimpleName();
            final var propertySchema = schema.getProperties() != null
                    ? schema.getProperties().get(name)
                    : null;

            if (propertySchema == null
                    || Boolean.FALSE.equals(propertySchema.getNullable())) {
                return;
            }

            if (modelProperty.getType() instanceof ClassOrInterfaceTypeExpr referenceType) {
                if (shouldAnnotate(referenceType)) {
                    referenceType.annotation(
                            new Annot()
                                    .name("org.checkerframework.checker.nullness.qual.Nullable")
                                    .target(AnnotTarget.FIELD)
                    );
                }
            }
        });
    }

    private boolean shouldAnnotate(final ClassOrInterfaceTypeExpr referenceType) {
        return !"org.openapitools.jackson.nullable.JsonNullable".equals(referenceType.getName());
    }

}
