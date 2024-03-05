package io.github.potjerodekool.openapi.common.generate;

import io.swagger.v3.oas.models.media.Schema;

public record ResolvedSchemaResult(String name, Schema<?> schema) {
}
