package io.github.potjerodekool.openapi.internal.generate.model.adapter;

import io.github.potjerodekool.openapi.internal.generate.model.model.element.Annotation;
import io.github.potjerodekool.openapi.internal.generate.model.model.element.AnnotationTarget;
import io.github.potjerodekool.openapi.internal.generate.model.model.element.Model;
import io.github.potjerodekool.openapi.internal.generate.model.model.type.ReferenceType;
import io.swagger.v3.oas.models.media.ObjectSchema;

public class CheckerModelAdapter implements ModelAdapter {
    @Override
    public void adapt(final Model model, final ObjectSchema schema) {
        adaptProperties(model, schema);
    }

    private void adaptProperties(final Model model,
                                 final ObjectSchema schema) {
        model.getProperties().forEach(modelProperty -> {
            final var name = modelProperty.getSimpleName();
            final var propertySchema = schema.getProperties().get(name);

            if (propertySchema == null
                    || Boolean.FALSE.equals(propertySchema.getNullable())) {
                return;
            }

            if (modelProperty.getType() instanceof ReferenceType referenceType) {
                if (shouldAnnotate(referenceType)) {
                    referenceType.annotation(
                            new Annotation()
                                    .name("org.checkerframework.checker.nullness.qual.Nullable")
                                    .annotationTarget(AnnotationTarget.FIELD)
                    );
                }
            }
        });
    }

    private boolean shouldAnnotate(final ReferenceType referenceType) {
        return !"org.openapitools.jackson.nullable.JsonNullable".equals(referenceType.getName());
    }

}
