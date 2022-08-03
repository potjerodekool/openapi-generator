package io.github.potjerodekool.openapi.tree;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public record OpenApiOperation(String summary,
                               String description,
                               String operationId,
                               List<String> tags,
                               List<OpenApiParameter> parameters,
                               @Nullable OpenApiRequestBody requestBody,
                               java.util.Map<String, OpenApiResponse> responses) {

}
