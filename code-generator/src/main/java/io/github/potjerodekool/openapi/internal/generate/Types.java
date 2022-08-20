package io.github.potjerodekool.openapi.internal.generate;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import io.github.potjerodekool.openapi.type.OpenApiType;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface Types {

    Type createType(OpenApiType openApiType);

    boolean isMapType(Type type);

    ClassOrInterfaceType createType(String name);

    ClassOrInterfaceType createListType();

    ClassOrInterfaceType createListType(Type typeArg);

    ClassOrInterfaceType createMapType();

    ClassOrInterfaceType createMapType(Type keyType, Type valueType);

    ClassOrInterfaceType createObjectType();

    ClassOrInterfaceType createVoidType();

    String getListTypeName();

    boolean isListTypeName(String typeName);

    boolean isListType(Type type);

    String getTypeName(Type type);

    boolean isStringType(final Type type);

    boolean isStringTypeName(final String typeName);

    ClassOrInterfaceType createStringType();

    ClassOrInterfaceType createCharSequenceType();


    ClassOrInterfaceType getBoxedType(Type type);

    boolean isAssignableBy(Type a,
                           Type b);

    CompilationUnit createCompilationUnit();

    Type createMultipartType();

    boolean isBooleanType(Type type);
}
