package io.github.potjerodekool.openapi.generate;

import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import io.github.potjerodekool.openapi.type.OpenApiType;

public interface Types {

    Type createType(OpenApiType openApiType);

    ClassOrInterfaceType createType(final String name);

    ClassOrInterfaceType getBoxedType(Type type);
}
