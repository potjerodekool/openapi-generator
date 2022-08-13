package io.github.potjerodekool.openapi.internal.generate;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.type.*;
import com.github.javaparser.resolution.SymbolResolver;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import io.github.potjerodekool.openapi.internal.util.Utils;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;
import io.github.potjerodekool.openapi.tree.Package;
import io.github.potjerodekool.openapi.type.OpenApiArrayType;
import io.github.potjerodekool.openapi.type.OpenApiObjectType;
import io.github.potjerodekool.openapi.type.OpenApiStandardType;
import io.github.potjerodekool.openapi.type.OpenApiType;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TypesJava implements Types {

    private static final String OBJECT_CLASS_NAME = "java.lang.Object";
    private static final String VOID_CLASS_NAME = "java.lang.Void";
    private static final String STRING_CLASS_NAME = "java.lang.String";
    private static final String LIST_CLASS_NAME = "java.util.List";

    private static final String MAP_CLASS_NAME = "java.util.Map";
    private final SymbolResolver symbolResolver;

    public TypesJava() {
        this.symbolResolver = new JavaSymbolSolver(new ReflectionTypeSolver());
    }

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

                if (isMapType(pck, ot.name(), ot.additionalProperties())) {
                    yield createMapType(ot);
                } else if (pck.isUnnamed()) {
                    yield createType(name);
                } else {
                    final var qualifiedName = pck.getName() + "." + name;
                    yield createType(qualifiedName);
                }
            }
        };
    }

    private boolean isMapType(final Package pck, final String name,
                              final @Nullable OpenApiProperty additionalProperties) {
        return pck.isUnnamed()
                && "object".equals(name)
                &&  additionalProperties != null;
    }

    private Type createMapType(final OpenApiObjectType ot) {
        final var additionalProperties = Utils.requireNonNull(ot.additionalProperties());
        final var additionalPropertiesType = additionalProperties.type();
        final var keyType = createStringType();
        final var valueType = createType(additionalPropertiesType);
        return createMapType(keyType, valueType);
    }

    @Override
    public boolean isMapType(final Type type) {
        return MAP_CLASS_NAME.equals(getTypeName(type));
    }

    private Type createStandardType(final OpenApiStandardType st) {
        return switch (st.typeEnum()) {
            case STRING -> {
                if (st.format() == null) {
                    yield createType(STRING_CLASS_NAME);
                }
                yield switch (st.format()) {
                    case "date" -> createType("java.time.LocalDate");
                    case "date-time" -> createType("java.time.LocalDateTime");
                    case "uuid" -> createType("java.util.UUID");
                    //Since we currently only support Spring we return a Spring specific type
                    case "binary"->
                        new WildcardType(createType("org.springframework.core.io.Resource"));
                    default -> createType(STRING_CLASS_NAME);
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

        type.setParentNode(createCompilationUnit());
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
    public ClassOrInterfaceType createMapType() {
        return createType(MAP_CLASS_NAME);
    }

    @Override
    public ClassOrInterfaceType createMapType(final Type keyType, final Type valueType) {
        return createMapType().setTypeArguments(keyType, valueType);
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
    public boolean isListType(final Type type) {
        if (type instanceof ClassOrInterfaceType ct) {
            return isListTypeName(ct.getNameWithScope());
        }
        return false;
    }

    @Override
    public String getTypeName(final Type type) {
        if (type instanceof ClassOrInterfaceType ct) {
            return ct.getNameWithScope();
        } else if (type instanceof PrimitiveType pt) {
            return switch (pt.getType()) {
                case BOOLEAN -> "boolean";
                case CHAR -> "char";
                case BYTE -> "byte";
                case SHORT -> "short";
                case INT -> "int";
                case LONG -> "long";
                case FLOAT -> "float";
                case DOUBLE -> "double";
            };
        }

        return "";
    }

    @Override
    public boolean isStringTypeName(final String typeName) {
        return STRING_CLASS_NAME.equals(typeName);
    }

    @Override
    public ClassOrInterfaceType createStringType() {
        return createType(STRING_CLASS_NAME);
    }

    @Override
    public ClassOrInterfaceType createCharSequenceType() {
        return createType("java.lang.CharSequence");
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

    @Override
    public Type createMultipartType() {
        return createType("org.springframework.web.multipart.MultipartFile");
    }

    @Override
    public boolean isAssignableBy(final Type a,
                                  final Type b) {
        if (!(a instanceof ClassOrInterfaceType)) {
            return false;
        }

        if (!(b instanceof ClassOrInterfaceType)) {
            return false;
        }

        try {
            final var resolvedA = a.resolve();
            final var resolvedB = b.resolve();

            return resolvedA.isAssignableBy(resolvedB);
        } catch (final Exception e) {
            //TODO Fix classes that are not found on the classpath like org.openapitools.jackson.nullable.JsonNullable
            return false;
        }
    }

    @Override
    public CompilationUnit createCompilationUnit() {
        final var cu = new CompilationUnit();
        cu.setData(Node.SYMBOL_RESOLVER_KEY, symbolResolver);
        return cu;
    }
}
