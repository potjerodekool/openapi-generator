package io.github.potjerodekool.openapi.internal.ast.util;

import io.github.potjerodekool.openapi.internal.ast.element.*;
import io.github.potjerodekool.openapi.internal.ast.type.java.JavaErrorType;
import io.github.potjerodekool.openapi.internal.ast.type.java.JavaPrimitiveType;
import io.github.potjerodekool.openapi.internal.ast.type.java.JavaArrayTypeImpl;
import io.github.potjerodekool.openapi.internal.ast.type.java.WildcardType;
import io.github.potjerodekool.openapi.internal.ast.type.kotlin.KotlinArrayType;
import io.github.potjerodekool.openapi.internal.loader.TypeElementLoader;
import io.github.potjerodekool.openapi.internal.loader.ReflectionTypeElementLoader;
import io.github.potjerodekool.openapi.internal.ast.type.*;
import io.github.potjerodekool.openapi.internal.util.GenerateException;
import io.github.potjerodekool.openapi.internal.util.QualifiedName;
import io.github.potjerodekool.openapi.internal.util.Utils;
import static io.github.potjerodekool.openapi.internal.util.Utils.isNullOrTrue;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;
import io.github.potjerodekool.openapi.tree.Package;
import io.github.potjerodekool.openapi.type.OpenApiArrayType;
import io.github.potjerodekool.openapi.type.OpenApiObjectType;
import io.github.potjerodekool.openapi.type.OpenApiStandardType;
import io.github.potjerodekool.openapi.type.OpenApiType;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.lang.model.element.ElementKind;
import javax.lang.model.type.TypeKind;
import java.util.List;
import java.util.Set;

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
            final var declaratedType = (DeclaredType) typeElement.asType();
            return declaratedType.copy();
        } catch (final ClassNotFoundException e) {
            final var qualifiedName = QualifiedName.from(className);
            final var packageName = qualifiedName.packageName();
            final var packageElement = PackageElement.create(packageName);
            final var typeElement = TypeElement.createClass(qualifiedName.simpleName());
            typeElement.setEnclosingElement(packageElement);
            return (DeclaredType) typeElement.asType();
        }
    }

    public Type<?> createType(final OpenApiType type) {
        return switch (type.kind()) {
            case STANDARD -> createStandardType((OpenApiStandardType) type);
            case ARRAY -> {
                final var at = (OpenApiArrayType) type;
                final var componentType = createType(at.items());

                if (componentType.isPrimitiveType()) {
                    yield createArray(componentType);
                }

                yield createListType(
                        (DeclaredType) componentType,
                        Utils.isNullOrTrue(at.nullable())
                );
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
                    case "time" -> createDeclaredType( "java.time.LocalTime", isNullable);
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
                            : createPrimitiveType(TypeKind.LONG);
                } else {
                    yield isNullable
                            ? createDeclaredType( "java.lang.Integer", true)
                            : createPrimitiveType(TypeKind.INT);
                }
            }
            case BOOLEAN -> isNullable
                            ? createDeclaredType( "java.lang.Boolean", true)
                            : createPrimitiveType(TypeKind.BOOLEAN);
            case NUMBER -> {
                if ("double".equals(st.format())) {
                    yield isNullable
                            ? createDeclaredType( "java.lang.Double", true)
                            : createPrimitiveType(TypeKind.DOUBLE);
                } else  {
                    yield isNullable
                            ? createDeclaredType( "java.lang.Float", true)
                            : createPrimitiveType(TypeKind.FLOAT);
                }
            }
            case BYTE -> isNullable
                    ? createDeclaredType( "java.lang.Byte", true):
                     createPrimitiveType(TypeKind.BYTE);
            case SHORT -> isNullable
                    ? createDeclaredType( "java.lang.Short", true)
                    : createPrimitiveType(TypeKind.SHORT);
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
        final var additionalProperties = ot.additionalProperties();
        if (additionalProperties == null) {
            throw new IllegalArgumentException(String.format("type %s has no additionalProperties", ot.name()));
        }
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

    public DeclaredType createListType(final DeclaredType typeArg,
                                       final boolean isNullable) {
        final var listType = createListType(isNullable);
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

    public DeclaredType createMultipartType() {
        return createDeclaredType("org.springframework.web.multipart.MultipartFile");
    }

    public DeclaredType createObjectType() {
        return createDeclaredType(OBJECT_CLASS_NAME);
    }

    public PrimitiveType createPrimitiveType(final TypeKind kind) {
        return switch (kind) {
            case BOOLEAN,
                    BYTE,
                    SHORT,
                    INT,
                    LONG,
                    CHAR,
                    FLOAT,
                    DOUBLE -> {
                        final TypeElement typeElement = createPrimitiveElement();
                        final var type = new JavaPrimitiveType(kind, typeElement);
                        typeElement.setType(type);
                        yield type;
            }
            default -> throw new IllegalArgumentException(String.format("Not a primitive type kind %s", kind));
        };
    }

    private TypeElement createArrayElement() {
        final var typeElement = TypeElement.create(ElementKind.CLASS, List.of(), Set.of(), "array");
        final var field = VariableElement.createField("length", createPrimitiveType(TypeKind.INT), null);
        typeElement.addEnclosedElement(field);
        return typeElement;
    }

    private TypeElement createPrimitiveElement() {
        return TypeElement.createClass("primitive");
    }

    public io.github.potjerodekool.openapi.internal.ast.type.JavaArrayType createArray(final Type<?> componentType) {
        return new JavaArrayTypeImpl(componentType, createArrayElement(), false);
    }

    public KotlinArrayType createKotlinArray(final Type<?> componentType) {
        return new KotlinArrayType(componentType, createArrayElement(), false);
    }

    public ErrorType createErrorType() {
        return new JavaErrorType(TypeElement.createClass(""));
    }
}
