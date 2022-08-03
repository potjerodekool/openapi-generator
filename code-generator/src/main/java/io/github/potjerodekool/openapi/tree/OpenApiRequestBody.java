package io.github.potjerodekool.openapi.tree;

import io.github.potjerodekool.openapi.type.OpenApiType;

import java.util.Map;

public record OpenApiRequestBody(String description,
                                 Map<String, OpenApiType> contentMediaType,
                                 Boolean required) {

}
