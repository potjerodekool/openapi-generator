package io.github.potjerodekool.openapi.common.generate.model.adapter;

import io.github.potjerodekool.openapi.common.ApiConfiguration;
import io.github.potjerodekool.openapi.common.generate.model.element.Model;
import io.swagger.v3.oas.models.media.ObjectSchema;

import java.util.Map;

public interface ModelAdapter {
    void adapt(Model model,
               ObjectSchema schema,
               ApiConfiguration apiConfiguration);
}
