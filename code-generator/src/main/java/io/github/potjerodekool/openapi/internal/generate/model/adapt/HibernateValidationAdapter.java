package io.github.potjerodekool.openapi.internal.generate.model.adapt;

import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;

public class HibernateValidationAdapter extends StandardValidationModelAdapter {

    @Override
    protected void adaptField(final VariableDeclaration field, final Schema<?> propertySchema) {
        super.adaptField(field, propertySchema);
        processUniqueItems(field, propertySchema);
    }

    private void processUniqueItems(final VariableDeclaration field,
                                    final Schema<?> schema) {
        if (Boolean.TRUE.equals(schema.getUniqueItems())
                && schema instanceof ArraySchema) {
            field.annotation(new AnnotationExpression("org.hibernate.validator.constraints.UniqueElements"));
        }
    }
}
