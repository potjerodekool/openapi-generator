package io.github.potjerodekool.openapi.internal.generate.model;

import io.swagger.v3.oas.models.media.Schema;

public record ResolvedSchemaResult(String name, Schema<?> schema) {
}
