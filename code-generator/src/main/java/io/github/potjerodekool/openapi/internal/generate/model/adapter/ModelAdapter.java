package io.github.potjerodekool.openapi.internal.generate.model.adapter;

import io.github.potjerodekool.openapi.internal.generate.model.model.element.Model;
import io.swagger.v3.oas.models.media.ObjectSchema;

public interface ModelAdapter {
    void adapt(Model model, final ObjectSchema schema);
}
