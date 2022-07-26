package com.github.potjerodekool.openapi.generate;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.potjerodekool.openapi.type.*;
import com.github.potjerodekool.openapi.util.Utils;

import java.util.Map;

public class JavaTypes implements Types {

    private static final Map<String, String> TO_PLATFORM_TYPE_MAPPING = Map.of(
            "string", "java.lang.String",
            "date-time", "java.time.LocalDateTime",
            "date", "java.time.LocalDate"
    );

    @Override
    public Type createType(final OpenApiType type,
                           final boolean isNullable) {
        if (type instanceof OpenApiStandardType st) {
            return switch (st.getStandardTypeEnum()) {
                case STRING -> createType("java.lang.String");
                case INTEGER ->
                        isNullable
                            ? createType("java.lang.Integer")
                            : PrimitiveType.intType();
                case LONG ->
                        isNullable
                                ? createType("java.lang.Long")
                                : PrimitiveType.longType();
                case DATE -> createType("java.time.LocalDate");
                case DATE_TIME -> createType("java.time.LocalDateTime");
            };
        } else if (type instanceof OpenApiOtherType ot) {
            final var name = ot.name();
            final var format = ot.format();

            return switch (name) {
                case "integer" -> {
                    final var typeName = "int64".equals(format)
                            ? "java.lang.Long"
                            : "java.lang.Integer";
                    yield createClassOrInterfaceType(typeName);
                }
                default -> {
                    final var platformTypeName = TO_PLATFORM_TYPE_MAPPING.get(name);

                    if (platformTypeName != null) {
                        yield createClassOrInterfaceType(platformTypeName);
                    }

                    throw new IllegalArgumentException("" + type.name());
                }
            };
        } else if (type instanceof OpenApiArrayType at) {
            final var itemsType = createType(at.getItems(), false);
            final var listType = createClassOrInterfaceType("java.util.List");
            return listType.setTypeArguments(new NodeList<>(itemsType));
        } else if (type instanceof OpenApiObjectType ot) {
            final var name = Utils.firstUpper(ot.name());
            final var pck = ot.pck();

            if (pck.isUnnamed()) {
                return new ClassOrInterfaceType().setName(name);
            } else {
                final var qualifiedName = pck.getName() + "." + name;
                return createClassOrInterfaceType(qualifiedName);
            }
        } else {
            throw new IllegalArgumentException("" + type.getClass());
        }
    }

    public static ClassOrInterfaceType createClassOrInterfaceType(final String name) {
        final var elements = name.split("\\.");
        var scope = new ClassOrInterfaceType().setName(elements[0]);
        var type = scope;

        for (int i = 1; i < elements.length; i++) {
            scope = type;
            type = new ClassOrInterfaceType(scope, elements[i]);
        }

        return type;
    }

    @Override
    public ClassOrInterfaceType createType(final String name) {
        return createClassOrInterfaceType(name);
    }

}
