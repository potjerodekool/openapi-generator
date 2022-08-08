package io.github.potjerodekool.openapi.generate;

import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import io.github.potjerodekool.openapi.type.OpenApiType;

public interface Types {

    Type createType(OpenApiType openApiType);

    ClassOrInterfaceType createType(String name);

    ClassOrInterfaceType createListType(Type typeArg);

    ClassOrInterfaceType createObjectType();

    ClassOrInterfaceType createVoidType();

    String getListTypeName();

    boolean isListTypeName(String typeName);

    boolean isStringTypeName(final String typeName);

    ClassOrInterfaceType getBoxedType(Type type);
}
