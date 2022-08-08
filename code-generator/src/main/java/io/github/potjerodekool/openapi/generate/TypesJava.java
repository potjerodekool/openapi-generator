package io.github.potjerodekool.openapi.generate;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import io.github.potjerodekool.openapi.util.Utils;
import io.github.potjerodekool.openapi.type.OpenApiArrayType;
import io.github.potjerodekool.openapi.type.OpenApiObjectType;
import io.github.potjerodekool.openapi.type.OpenApiStandardType;
import io.github.potjerodekool.openapi.type.OpenApiType;

public class TypesJava implements Types {

    private static final String OBJECT_CLASS_NAME = "java.lang.Object";
    private static final String VOID_CLASS_NAME = "java.lang.Void";
    private static final String STRING_CLASS_NAME = "java.lang.String";
    private static final String LIST_CLASS_NAME = "java.util.List";

    @Override
    public Type createType(final OpenApiType type) {
        return switch (type.kind()) {
            case STANDARD -> createStandardType((OpenApiStandardType) type);
            case ARRAY -> {
                final var at = (OpenApiArrayType) type;
                final var componentType = createType(at.items());

                if (componentType.isPrimitiveType()) {
                    yield new ArrayType(componentType);
                }

                final var listType = createType(LIST_CLASS_NAME);
                yield listType.setTypeArguments(new NodeList<>(componentType));
            }
            case OBJECT -> {
                final var ot = (OpenApiObjectType) type;
                final var name = Utils.firstUpper(ot.name());
                final var pck = ot.pck();

                if (pck.isUnnamed()) {
                    yield new ClassOrInterfaceType().setName(name);
                } else {
                    final var qualifiedName = pck.getName() + "." + name;
                    yield createType(qualifiedName);
                }
            }
        };
    }

    private Type createStandardType(final OpenApiStandardType st) {
        return switch (st.typeEnum()) {
            case STRING -> {
                if (st.format() == null) {
                    yield createType("java.lang.String");
                }
                yield switch (st.format()) {
                    case "date" -> createType("java.time.LocalDate");
                    case "date-time" -> createType("java.time.LocalDateTime");
                    case "uuid" -> createType("java.util.UUID");
                    default -> createType("java.lang.String");
                };
            }
            case INTEGER -> {
                if ("int64".equals(st.format())) {
                    yield isNullOrTrue(st.nullable())
                            ? createType("java.lang.Long")
                            : PrimitiveType.longType();
                } else {
                    yield isNullOrTrue(st.nullable())
                            ? createType("java.lang.Integer")
                            : PrimitiveType.intType();
                }
            }
            case BOOLEAN ->
                    isNullOrTrue(st.nullable())
                            ? createType("java.lang.Boolean")
                            : PrimitiveType.booleanType();
            case NUMBER -> {
                if ("double".equals(st.format())) {
                    yield isNullOrTrue(st.nullable())
                            ? createType("java.lang.Double")
                            : PrimitiveType.doubleType();
                } else  {
                    yield isNullOrTrue(st.nullable())
                            ? createType("java.lang.Float")
                            : PrimitiveType.floatType();
                }
            }
        };
    }

    public ClassOrInterfaceType createType(final String name) {
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
    public String getListTypeName() {
        return LIST_CLASS_NAME;
    }

    @Override
    public ClassOrInterfaceType createListType(final Type typeArg) {
        return createType(LIST_CLASS_NAME).setTypeArguments(typeArg);
    }

    @Override
    public ClassOrInterfaceType createObjectType() {
        return createType(OBJECT_CLASS_NAME);
    }

    @Override
    public ClassOrInterfaceType createVoidType() {
        return createType(VOID_CLASS_NAME);
    }

    @Override
    public boolean isListTypeName(final String typeName) {
        return LIST_CLASS_NAME.equals(typeName);
    }

    @Override
    public boolean isStringTypeName(final String typeName) {
        return STRING_CLASS_NAME.equals(typeName);
    }

    private boolean isNullOrTrue(final Boolean value) {
        return value == null || Boolean.TRUE.equals(value);
    }

    @Override
    public ClassOrInterfaceType getBoxedType(final Type type) {
        if (type.isPrimitiveType()) {
            final var primitiveType = (PrimitiveType) type;
            return switch (primitiveType.getType()) {
                case BOOLEAN -> createType("java.lang.Boolean");
                case CHAR -> createType("java.lang.Character");
                case BYTE -> createType("java.lag.Byte");
                case SHORT -> createType("java.lang.Short");
                case INT -> createType("java.lang.Integer");
                case LONG -> createType("java.lang.Long");
                case FLOAT -> createType("java.lang.Float");
                case DOUBLE -> createType("java.lang.Double");
            };
        } else {
            return (ClassOrInterfaceType) type;
        }
    }
}
