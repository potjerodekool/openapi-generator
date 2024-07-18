package io.github.potjerodekool.openapi.common.generate.model.adapter;

import io.github.potjerodekool.openapi.common.ApiConfiguration;
import io.github.potjerodekool.openapi.common.generate.model.element.Model;
import io.github.potjerodekool.openapi.common.generate.model.element.ModelProperty;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;

import java.util.Optional;

public abstract class AbstractModelAdapter implements ModelAdapter {
    @Override
    public void adapt(final Model model,
                      final ObjectSchema schema,
                      final ApiConfiguration apiConfiguration) {
        adaptProperties(model, schema, apiConfiguration);
    }

    private void adaptProperties(final Model model,
                                 final ObjectSchema schema,
                                 final ApiConfiguration apiConfiguration) {
        model.getProperties().forEach(modelProperty ->
                adaptProperty(modelProperty, schema, apiConfiguration));
    }

    protected void adaptProperty(final ModelProperty modelProperty,
                                 final ObjectSchema schema,
                                 final ApiConfiguration apiConfiguration) {
    }

    protected Optional<Schema<?>> findPropertySchema(final ObjectSchema schema,
                                                     final String name) {
        return schema.getProperties() != null
                ? Optional.ofNullable(schema.getProperties().get(name))
                : Optional.empty();
    }
}
