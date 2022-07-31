package com.github.potjerodekool.openapi.tree;

import com.github.potjerodekool.openapi.type.OpenApiType;

import java.util.Map;

public record OpenApiRequestBody(String description,
                                 Map<String, OpenApiType> contentMediaType,
                                 Boolean required) {

}
