package io.github.potjerodekool.openapi.common.generate.model.adapter;

import io.github.potjerodekool.openapi.common.generate.model.element.Annotation;
import io.github.potjerodekool.openapi.common.generate.model.element.AnnotationTarget;
import io.github.potjerodekool.openapi.common.generate.model.element.ModelProperty;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;

public class HibernateValidationModelAdapter extends JakartaValidationModelAdapter {

    @Override
    protected void adaptProperty(final Schema<?> propertySchema, final ModelProperty property) {
        super.adaptProperty(propertySchema, property);
        processUniqueItems(propertySchema, property);
    }

    private void processUniqueItems(final Schema<?> schema,
                                    final ModelProperty property) {
        if (Boolean.TRUE.equals(schema.getUniqueItems())
                && schema instanceof ArraySchema) {
            property.annotation(new Annotation()
                    .name("org.hibernate.validator.constraints.UniqueElements")
                    .annotationTarget(AnnotationTarget.FIELD)
            );
        }
    }
}
