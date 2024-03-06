package io.github.potjerodekool.openapi.common.util;


import io.swagger.v3.oas.models.media.Schema;

import java.util.Map;

public record SchemaAndExtensions(Schema<?> schema, Map<String, Object> extensions) {

}
