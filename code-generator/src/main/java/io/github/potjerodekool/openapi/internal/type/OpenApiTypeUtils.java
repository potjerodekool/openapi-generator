package io.github.potjerodekool.openapi.internal.type;

import io.github.potjerodekool.codegen.model.symbol.ErrorSymbol;
import io.github.potjerodekool.codegen.model.symbol.PackageSymbol;
import io.github.potjerodekool.codegen.model.tree.expression.Expression;
import io.github.potjerodekool.codegen.model.tree.expression.NameExpression;
import io.github.potjerodekool.codegen.model.tree.type.*;
import io.github.potjerodekool.codegen.model.type.*;
import io.github.potjerodekool.codegen.model.util.Elements;
import io.github.potjerodekool.codegen.model.util.QualifiedName;
import io.github.potjerodekool.codegen.model.util.StringUtils;
import io.github.potjerodekool.codegen.model.util.type.Types;
import io.github.potjerodekool.openapi.internal.util.TypeUtils;
import io.github.potjerodekool.openapi.internal.util.Utils;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;
import io.github.potjerodekool.openapi.tree.Package;
import io.github.potjerodekool.openapi.type.OpenApiArrayType;
import io.github.potjerodekool.openapi.type.OpenApiObjectType;
import io.github.potjerodekool.openapi.type.OpenApiStandardType;
import io.github.potjerodekool.openapi.type.OpenApiType;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public class OpenApiTypeUtils {

    private final Types types;
    private final TypeUtils typeUtils;
    private final Elements elements;

    public OpenApiTypeUtils(final Types types,
                            final TypeUtils typeUtils,
                            final Elements elements) {
        this.types = types;
        this.typeUtils = typeUtils;
        this.elements = elements;
    }

    private DeclaredType createDeclaredType(final String className,
                                            final boolean isNullable) {
        final var element = elements.getTypeElement(className);

        if (element == null) {
            final var errorSymbol = ErrorSymbol.create();
            final var qualifiedName = QualifiedName.from(className);
            errorSymbol.setSimpleName(qualifiedName.simpleName());
            errorSymbol.setEnclosingElement(PackageSymbol.create(qualifiedName.packageName()));
            return (DeclaredType) errorSymbol.asType();
        }

        final var type = element.asType();
        return (DeclaredType) (isNullable ? type.asNullableType() : type);
    }

    public TypeExpression createTypeExpression(final OpenApiType type) {
        return switch (type.kind()) {
            case STANDARD -> createStandardTypeExpression((OpenApiStandardType) type);
            case ARRAY -> {
                final var at = (OpenApiArrayType) type;
                final var componentType = createTypeExpression(at.items());

                if (componentType instanceof PrimitiveTypeExpression) {
                    yield new ArrayTypeExpression(componentType);
                }

                yield createAnnotatedType("java.util.List",
                        List.of(componentType),
                        Utils.isNullOrTrue(at.nullable()));
            }
            case OBJECT -> {
                final var ot = (OpenApiObjectType) type;
                final var name = StringUtils.firstUpper(ot.name());
                final var pck = ot.pck();

                if (isMapType(pck, ot.name(), ot.additionalProperties())) {
                    yield createMapTypeExpression(ot);
                } else if (pck.isUnnamed()) {
                    if ("object".equals(ot.name())) {
                        yield new AnnotatedTypeExpression(
                                new NameExpression("java.lang.Object"),
                                List.of(),
                                Boolean.TRUE.equals(ot.nullable())
                        );
                    } else {
                        yield new AnnotatedTypeExpression(
                                new NameExpression(name),
                                List.of(),
                                Boolean.TRUE.equals(ot.nullable())
                        );
                    }
                } else {
                    final var qualifiedName = pck.getName() + "." + name;
                    yield new AnnotatedTypeExpression(
                            new NameExpression(qualifiedName),
                            List.of(),
                            Boolean.TRUE.equals(ot.nullable())
                    );
                }
            }
        };
    }

    public TypeMirror createType(final OpenApiType type) {
        return switch (type.kind()) {
            case STANDARD -> createStandardType((OpenApiStandardType) type);
            case ARRAY -> {
                final var at = (OpenApiArrayType) type;
                final var componentType = createType(at.items());

                if (componentType.isPrimitiveType()) {
                    yield types.getArrayType(componentType);
                }

                yield typeUtils.createListType(
                        (DeclaredType) componentType,
                        Utils.isNullOrTrue(at.nullable())
                );
            }
            case OBJECT -> {
                final var ot = (OpenApiObjectType) type;
                final var name = StringUtils.firstUpper(ot.name());
                final var pck = ot.pck();

                if (isMapType(pck, ot.name(), ot.additionalProperties())) {
                    yield createMapType(ot);
                } else if (pck.isUnnamed()) {
                    if ("object".equals(ot.name())) {
                        yield typeUtils.createObjectType(Boolean.TRUE.equals(ot.nullable()));
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

    private TypeExpression createStandardTypeExpression(final OpenApiStandardType st) {
        var isNullable = Boolean.TRUE.equals(st.nullable());

        return switch (st.typeEnum()) {
            case STRING -> {
                if (st.format() == null) {
                    yield createAnnotatedType("java.lang.String", isNullable);
                }
                yield switch (st.format()) {
                    case "date" -> createAnnotatedType("java.time.LocalDate", isNullable);
                    case "date-time" -> createAnnotatedType( "java.time.LocalDateTime", isNullable);
                    case "time" -> createAnnotatedType( "java.time.LocalTime", isNullable);
                    case "uuid" -> createAnnotatedType("java.util.UUID", isNullable);
                    //Since we currently only support Spring we return a Spring specific type
                    case "binary"-> new WildCardTypeExpression(
                            BoundKind.EXTENDS,
                            createAnnotatedType("org.springframework.core.io.Resource", isNullable)
                    );
                    default -> createAnnotatedType("java.lang.String", isNullable);
                };
            }
            case INTEGER -> {
                if ("int64".equals(st.format())) {
                    yield isNullable
                            ? createAnnotatedType( "java.lang.Long", true)
                            : new PrimitiveTypeExpression(TypeKind.LONG);
                } else {
                    yield isNullable
                            ? createAnnotatedType( "java.lang.Integer", true)
                            : new PrimitiveTypeExpression(TypeKind.INT);
                }
            }
            case BOOLEAN -> isNullable
                    ? createAnnotatedType( "java.lang.Boolean", true)
                    : new PrimitiveTypeExpression(TypeKind.BOOLEAN);
            case NUMBER -> {
                if ("double".equals(st.format())) {
                    yield isNullable
                            ? createAnnotatedType( "java.lang.Double", true)
                            : new PrimitiveTypeExpression(TypeKind.DOUBLE);
                } else  {
                    yield isNullable
                            ? createAnnotatedType( "java.lang.Float", true)
                            : new PrimitiveTypeExpression(TypeKind.FLOAT);
                }
            }
            case BYTE -> isNullable
                    ? createAnnotatedType( "java.lang.Byte", true):
                    new PrimitiveTypeExpression(TypeKind.BYTE);
            case SHORT -> isNullable
                    ? createAnnotatedType( "java.lang.Short", true)
                    : new PrimitiveTypeExpression(TypeKind.SHORT);
        };
    }

    private AnnotatedTypeExpression createAnnotatedType(final String name,
                                                        final boolean isNullable) {
        return new AnnotatedTypeExpression(
                new NameExpression(name),
                List.of(),
                isNullable
        );
    }

    private AnnotatedTypeExpression createAnnotatedType(final String name,
                                                        final List<Expression> typeArguments,
                                                        final boolean isNullable) {
        final var paramType = new ParameterizedType(
                new NameExpression(name),
                typeArguments
        );

        return new AnnotatedTypeExpression(
                paramType,
                List.of(),
                isNullable
        );
    }

    private TypeMirror createStandardType(final OpenApiStandardType st) {
        var isNullable = Boolean.TRUE.equals(st.nullable());

        return switch (st.typeEnum()) {
            case STRING -> {
                if (st.format() == null) {
                    yield typeUtils.getStringType(isNullable);
                }
                yield switch (st.format()) {
                    case "date" -> typeUtils.getDeclaredType("java.time.LocalDate", isNullable);
                    case "date-time" -> typeUtils.getDeclaredType( "java.time.LocalDateTime", isNullable);
                    case "time" -> typeUtils.getDeclaredType( "java.time.LocalTime", isNullable);
                    case "uuid" -> typeUtils.getDeclaredType("java.util.UUID", isNullable);
                    //Since we currently only support Spring we return a Spring specific type
                    case "binary"-> WildcardType.withExtendsBound(
                            typeUtils.getDeclaredType("org.springframework.core.io.Resource", isNullable)
                        );
                    default -> typeUtils.getStringType(isNullable);
                };
            }
            case INTEGER -> {
                if ("int64".equals(st.format())) {
                    yield isNullable
                            ? typeUtils.getDeclaredType( "java.lang.Long", true)
                            : createPrimitiveType(TypeKind.LONG);
                } else {
                    yield isNullable
                            ? typeUtils.getDeclaredType( "java.lang.Integer", true)
                            : createPrimitiveType(TypeKind.INT);
                }
            }
            case BOOLEAN -> isNullable
                            ? typeUtils.getDeclaredType( "java.lang.Boolean", true)
                            : createPrimitiveType(TypeKind.BOOLEAN);
            case NUMBER -> {
                if ("double".equals(st.format())) {
                    yield isNullable
                            ? typeUtils.getDeclaredType( "java.lang.Double", true)
                            : createPrimitiveType(TypeKind.DOUBLE);
                } else  {
                    yield isNullable
                            ? typeUtils.getDeclaredType( "java.lang.Float", true)
                            : createPrimitiveType(TypeKind.FLOAT);
                }
            }
            case BYTE -> isNullable
                    ? typeUtils.getDeclaredType( "java.lang.Byte", true):
                     createPrimitiveType(TypeKind.BYTE);
            case SHORT -> isNullable
                    ? typeUtils.getDeclaredType( "java.lang.Short", true)
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

    private TypeMirror createMapType(final OpenApiObjectType ot) {
        final var additionalProperties = ot.additionalProperties();
        if (additionalProperties == null) {
            throw new IllegalArgumentException(String.format("type %s has no additionalProperties", ot.name()));
        }
        final var additionalPropertiesType = additionalProperties.type();
        final var keyType = typeUtils.getStringType(Boolean.TRUE.equals(ot.nullable()));
        final var valueType = (DeclaredType) createType(additionalPropertiesType);
        return typeUtils.createMapType(keyType, valueType, Boolean.TRUE.equals(ot.nullable()));
    }

    private TypeExpression createMapTypeExpression(final OpenApiObjectType ot) {
        final var additionalProperties = ot.additionalProperties();
        if (additionalProperties == null) {
            throw new IllegalArgumentException(String.format("type %s has no additionalProperties", ot.name()));
        }
        final var additionalPropertiesType = additionalProperties.type();
        final var keyType = createStringTypeExpression(Boolean.TRUE.equals(ot.nullable()));
        final var valueType = createTypeExpression(additionalPropertiesType);

        return createAnnotatedType(
                "java.util.Map",
                List.of(keyType, valueType),
                Boolean.TRUE.equals(ot.nullable())
        );
    }

    private PrimitiveType createPrimitiveType(final TypeKind kind) {
        return types.getPrimitiveType(kind);
    }

    private AnnotatedTypeExpression createStringTypeExpression(final boolean isNullable) {
        return createAnnotatedType("java.lang.String", isNullable);
    }
}
