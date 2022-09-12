package io.github.potjerodekool.openapi.internal.ast;

import io.github.potjerodekool.openapi.internal.ast.element.PackageElement;
import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;
import io.github.potjerodekool.openapi.internal.loader.TypeElementLoader;
import io.github.potjerodekool.openapi.internal.loader.ReflectionTypeElementLoader;
import io.github.potjerodekool.openapi.internal.ast.type.*;
import io.github.potjerodekool.openapi.internal.util.GenerateException;
import io.github.potjerodekool.openapi.internal.util.Utils;
import static io.github.potjerodekool.openapi.internal.util.Utils.isNullOrTrue;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;
import io.github.potjerodekool.openapi.tree.Package;
import io.github.potjerodekool.openapi.type.OpenApiArrayType;
import io.github.potjerodekool.openapi.type.OpenApiObjectType;
import io.github.potjerodekool.openapi.type.OpenApiStandardType;
import io.github.potjerodekool.openapi.type.OpenApiType;
import org.checkerframework.checker.nullness.qual.Nullable;

public class TypeUtils {

    private static final String OBJECT_CLASS_NAME = "java.lang.Object";
    private static final String STRING_CLASS_NAME = "java.lang.String";
    private static final String LIST_CLASS_NAME = "java.util.List";
    private static final String MAP_CLASS_NAME = "java.util.Map";
    private static final String CHAR_SEQUENCE_NAME = "java.lang.CharSequence";
    private static final String BOOLEAN_NAME = "java.lang.Boolean";

    private final TypeElementLoader reflectionClassLoader;

    public TypeUtils(final @Nullable ClassLoader classLoader) {
        final ClassLoader loader;
        if (classLoader != null) {
            loader = classLoader;
        } else {
            loader = ClassLoader.getSystemClassLoader();
        }

        this.reflectionClassLoader = new ReflectionTypeElementLoader(loader);
    }

    public DeclaredType createStringType() {
        return createDeclaredType(STRING_CLASS_NAME, false);
    }

    public DeclaredType createVoidType() {
        return createDeclaredType("java.lang.Void");
    }

    private DeclaredType createStringType(final boolean isNullable) {
        return createDeclaredType(STRING_CLASS_NAME, isNullable);
    }

    private DeclaredType createDeclaredType(final String className,
                                           final boolean isNullable) {
        final var type = createDeclaredType(className);
        return isNullable ? type.asNullableType() : type;
    }

    public DeclaredType createDeclaredType(final String className) {
        try {
            final var typeElement = reflectionClassLoader.loadTypeElement(className);
            return typeElement.asType();
        } catch (final ClassNotFoundException e) {
            final var qualifiedName = Utils.resolveQualifiedName(className);
            final var packageName = qualifiedName.packageName();
            final var packageElement = PackageElement.create(packageName);
            final var typeElement = TypeElement.createClass(qualifiedName.simpleName());
            typeElement.setEnclosingElement(packageElement);
            return typeElement.asType();
        }
    }

    public Type<?> createType(final OpenApiType type) {
        return switch (type.kind()) {
            case STANDARD -> createStandardType((OpenApiStandardType) type);
            case ARRAY -> {
                final var at = (OpenApiArrayType) type;
                final var componentType = createType(at.items());

                if (componentType.isPrimitiveType()) {
                    yield new JavaArrayType(componentType);
                }

                final var listType = createListType(
                        Boolean.TRUE.equals(type.nullable())
                );
                yield listType.withTypeArgument(componentType);
            }
            case OBJECT -> {
                final var ot = (OpenApiObjectType) type;
                final var name = Utils.firstUpper(ot.name());
                final var pck = ot.pck();

                if (isMapType(pck, ot.name(), ot.additionalProperties())) {
                    yield createMapType(ot);
                } else if (pck.isUnnamed()) {
                    if ("object".equals(ot.name())) {
                        yield createDeclaredType(OBJECT_CLASS_NAME, Boolean.TRUE.equals(ot.nullable()));
                    } else {
                        yield createDeclaredType(name, Boolean.TRUE.equals(ot.nullable()));
                    }
                } else {
                    final var qualifiedName = pck.getName() + "." + name;
                    yield createDeclaredType(qualifiedName, Boolean.TRUE.equals(ot.nullable()));
                }
            }
        };
    }

    private Type<?> createStandardType(final OpenApiStandardType st) {
        var isNullable = isNullOrTrue(st.nullable());

        return switch (st.typeEnum()) {
            case STRING -> {
                if (st.format() == null) {
                    yield createDeclaredType(STRING_CLASS_NAME, isNullable);
                }
                yield switch (st.format()) {
                    case "date" -> createDeclaredType("java.time.LocalDate", isNullable);
                    case "date-time" -> createDeclaredType( "java.time.LocalDateTime", isNullable);
                    case "uuid" -> createDeclaredType("java.util.UUID", isNullable);
                    //Since we currently only support Spring we return a Spring specific type
                    case "binary"-> WildcardType.withExtendsBound(
                            createDeclaredType("org.springframework.core.io.Resource", isNullable)
                        );
                    default -> createDeclaredType(STRING_CLASS_NAME, isNullable);
                };
            }
            case INTEGER -> {
                if ("int64".equals(st.format())) {
                    yield isNullable
                            ? createDeclaredType( "java.lang.Long", true)
                            : PrimitiveType.LONG;
                } else {
                    yield isNullable
                            ? createDeclaredType( "java.lang.Integer", true)
                            : PrimitiveType.INT;
                }
            }
            case BOOLEAN -> isNullable
                            ? createDeclaredType( "java.lang.Boolean", true)
                            : PrimitiveType.BOOLEAN;
            case NUMBER -> {
                if ("double".equals(st.format())) {
                    yield isNullable
                            ? createDeclaredType( "java.lang.Double", true)
                            : PrimitiveType.DOUBLE;
                } else  {
                    yield isNullable
                            ? createDeclaredType( "java.lang.Float", true)
                            : PrimitiveType.FLOAT;
                }
            }
            case BYTE -> isNullable
                    ? createDeclaredType( "java.lang.Byte", true)
                    : PrimitiveType.BYTE;
            case SHORT -> isNullable
                    ? createDeclaredType( "java.lang.Short", true)
                    : PrimitiveType.SHORT;
        };
    }

    private boolean isMapType(final Package pck,
                              final String name,
                              final @Nullable OpenApiProperty additionalProperties) {
        return pck.isUnnamed()
                && "object".equals(name)
                &&  additionalProperties != null;
    }

    public boolean isMapType(final Type<?> type) {
        return isDeclaredTypeWithName(type, MAP_CLASS_NAME);
    }

    private boolean isDeclaredTypeWithName(final Type<?> type,
                                           final String name) {
        if (type instanceof DeclaredType dt) {
            return name.equals(dt.getElement().getQualifiedName());
        } else {
            return false;
        }
    }

    private Type<?> createMapType(final OpenApiObjectType ot) {
        final var additionalProperties = Utils.requireNonNull(ot.additionalProperties());
        final var additionalPropertiesType = additionalProperties.type();
        final var keyType = createStringType(Boolean.TRUE.equals(ot.nullable()));
        final var valueType = (DeclaredType) createType(additionalPropertiesType);
        return createMapType(keyType, valueType, Boolean.TRUE.equals(ot.nullable()));
    }

    private DeclaredType createMapType(final DeclaredType keyType,
                                      final DeclaredType valueType,
                                      final boolean isNullable) {
        return createMapType(isNullable).withTypeArguments(keyType, valueType);
    }

    public DeclaredType createMapType() {
        return createMapType(false);
    }

    private DeclaredType createMapType(final boolean isNullable) {
        return createDeclaredType(MAP_CLASS_NAME, isNullable);
    }

    public DeclaredType getBoxedType(final Type<?> type) {
        if (type.isPrimitiveType()) {
            final var primitiveType = (PrimitiveType) type;
            return switch (primitiveType.getKind()) {
                case BOOLEAN -> createDeclaredType("java.lang.Boolean", false);
                case CHAR -> createDeclaredType("java.lang.Character", false);
                case BYTE -> createDeclaredType("java.lang.Byte", false);
                case SHORT -> createDeclaredType("java.lang.Short", false);
                case INT -> createDeclaredType("java.lang.Integer", false);
                case LONG -> createDeclaredType("java.lang.Long", false);
                case FLOAT -> createDeclaredType("java.lang.Float", false);
                case DOUBLE -> createDeclaredType("java.lang.Double", false);
                default -> throw new GenerateException(String.format("%s is not a primitive", type.getKind()));
            };
        }
        return (DeclaredType) type;
    }

    public boolean isListType(final Type<?> type) {
        return isDeclaredTypeWithName(type, LIST_CLASS_NAME);
    }

    public DeclaredType createListType() {
        return createDeclaredType(LIST_CLASS_NAME, false);
    }

    public DeclaredType createListType(final DeclaredType typeArg) {
        final var listType = createListType();
        return listType.withTypeArgument(typeArg);
    }

    private DeclaredType createListType(final boolean isNullable) {
        return createDeclaredType(LIST_CLASS_NAME, isNullable);
    }

    public String getListTypeName() {
        return LIST_CLASS_NAME;
    }

    public boolean isStringType(final Type<?> type) {
        return isDeclaredTypeWithName(type, STRING_CLASS_NAME);
    }

    public DeclaredType createCharSequenceType() {
        return createDeclaredType(CHAR_SEQUENCE_NAME, false);
    }

    public boolean isBooleanType(final Type<?> type) {
        return isDeclaredTypeWithName(type, BOOLEAN_NAME) || type.getKind() == TypeKind.BOOLEAN;
    }

    public String getTypeName(final Type<?> type) {
        if (type.isDeclaredType()) {
            return ((DeclaredType)type).getElement().getQualifiedName();
        }
        //TODO
        throw new UnsupportedOperationException("");
    }

    public DeclaredType createMultipartType() {
        return createDeclaredType("org.springframework.web.multipart.MultipartFile");
    }

    public DeclaredType createObjectType() {
        return createDeclaredType(OBJECT_CLASS_NAME);
    }
}
