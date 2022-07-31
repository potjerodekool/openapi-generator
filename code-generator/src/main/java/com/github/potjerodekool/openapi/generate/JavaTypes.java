package com.github.potjerodekool.openapi.generate;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.potjerodekool.openapi.type.*;
import com.github.potjerodekool.openapi.util.Utils;

public class JavaTypes implements Types {

    @Override
    public Type createType(final OpenApiType type,
                           final boolean isNullable) {
        if (type instanceof OpenApiStandardType st) {
            return switch (st.type()) {
                case "string" -> createType("java.lang.String");
                case "integer" -> {
                    if ("int64".equals(st.format())) {
                        yield isNullable
                                ? createType("java.lang.Long")
                                : PrimitiveType.longType();
                    } else {
                        yield isNullable
                                ? createType("java.lang.Integer")
                                : PrimitiveType.intType();
                    }
                }
                case "date" -> createType("java.time.LocalDate");
                case "date-time" -> createType("java.time.LocalDateTime");
                default -> throw new UnsupportedOperationException(String.format("unsupported type %s", st.type()));
            };
        } else if (type instanceof OpenApiOtherType) {
            throw new IllegalArgumentException("" + type.name());
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
