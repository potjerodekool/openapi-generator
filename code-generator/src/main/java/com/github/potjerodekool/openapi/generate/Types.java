package com.github.potjerodekool.openapi.generate;

import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import com.github.potjerodekool.openapi.type.OpenApiType;

public interface Types {

    Type createType(OpenApiType openApiType, boolean isNullable);

    ClassOrInterfaceType createType(String name);

}
