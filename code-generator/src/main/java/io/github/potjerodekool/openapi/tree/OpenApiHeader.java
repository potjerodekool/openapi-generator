package io.github.potjerodekool.openapi.tree;

import io.github.potjerodekool.openapi.type.OpenApiType;

public record OpenApiHeader(String description,
                            Boolean required,
                            Boolean deprecated,
                            Boolean allowEmptyValue,
                            String style,
                            Boolean explode,
                            Boolean allowReserved,
                            OpenApiType type) {

}
