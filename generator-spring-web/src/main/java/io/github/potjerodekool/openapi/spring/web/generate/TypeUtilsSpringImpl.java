package io.github.potjerodekool.openapi.spring.web.generate;

import io.github.potjerodekool.codegen.model.type.TypeKind;
import io.github.potjerodekool.codegen.template.model.type.*;
import io.github.potjerodekool.openapi.common.generate.ExtensionsHelper;
import io.github.potjerodekool.openapi.common.generate.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.common.generate.ResolvedSchemaResult;
import io.github.potjerodekool.openapi.common.generate.SchemaResolver;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.*;

import java.util.List;
import java.util.Map;

public class TypeUtilsSpringImpl implements OpenApiTypeUtils {
    @Override
    public TypeExpr createType(final OpenAPI openAPI,
                               final Schema<?> schema,
                               final Map<String, Object> extensions,
                               final String packageName,
                               final String mediaType,
                               final Boolean isRequired) {
        return switch (schema) {
            case ArraySchema arraySchema -> createArrayType(openAPI, arraySchema, packageName, mediaType);
            case BinarySchema ignored -> new ClassOrInterfaceTypeExpr("org.springframework.core.io.Resource");
            case BooleanSchema booleanSchema -> createBooleanTypeExpr(booleanSchema, isRequired);
            case ByteArraySchema ignored -> throw new UnsupportedOperationException();
            case DateSchema ignored -> createDateTypeExpr();
            case DateTimeSchema ignored -> createDateTimeTypeExpr();
            case EmailSchema ignored -> createStringTypeExpr();
            case FileSchema ignored -> throw new UnsupportedOperationException();
            case IntegerSchema integerSchema -> createIntegerType(integerSchema, isRequired);
            case JsonSchema ignored -> throw new UnsupportedOperationException();
            case MapSchema mapSchema -> createMapType(openAPI, mapSchema, packageName, mediaType, isRequired);
            case NumberSchema numberSchema -> createNumberType(numberSchema, isRequired);
            case ObjectSchema ignored -> {
                final var type = new ClassOrInterfaceTypeExpr("java.lang.Object");
                processExtensions(openAPI, type, packageName, extensions);
                yield type;
            }
            case PasswordSchema ignored -> createStringTypeExpr();
            case StringSchema ignored -> createStringTypeExpr();
            case UUIDSchema ignored -> createUuidTypeExpr();
            default -> createTypeDefault(
                    openAPI,
                    schema,
                    extensions,
                    packageName,
                    mediaType,
                    isRequired);
        };
    }

    private TypeExpr createNumberType(final NumberSchema numberSchema,
                                      final Boolean isRequired) {
        final var isNullable = Boolean.TRUE.equals(numberSchema.getNullable());

        if ("double".equals(numberSchema.getFormat())) {
            return isNullable || Boolean.FALSE.equals(isRequired)
                    ? new ClassOrInterfaceTypeExpr("java.lang.Double")
                    : new PrimitiveTypeExpr(TypeKind.DOUBLE);
        } else {
            return isNullable || Boolean.FALSE.equals(isRequired)
                    ? new ClassOrInterfaceTypeExpr("java.lang.Float")
                    : new PrimitiveTypeExpr(TypeKind.FLOAT);
        }
    }

    private TypeExpr createUuidTypeExpr() {
        return new ClassOrInterfaceTypeExpr("java.util.UUID");
    }

    private TypeExpr createStringTypeExpr() {
        return new ClassOrInterfaceTypeExpr("java.lang.String");
    }

    private TypeExpr createDateTimeTypeExpr() {
        return new ClassOrInterfaceTypeExpr("java.time.OffsetDateTime");
    }

    private TypeExpr createDateTypeExpr() {
        return new ClassOrInterfaceTypeExpr("java.time.LocalDate");
    }

    private TypeExpr createBooleanTypeExpr(final BooleanSchema booleanSchema,
                                           final Boolean isRequired) {
        final var isNullable = Boolean.TRUE.equals(booleanSchema.getNullable());
        return isNullable || Boolean.FALSE.equals(isRequired)
                ? new ClassOrInterfaceTypeExpr("java.lang.Boolean")
                : new PrimitiveTypeExpr(TypeKind.BOOLEAN);
    }

    private TypeExpr createIntegerType(final IntegerSchema integerSchema,
                                       final Boolean isRequired) {
        final var isNullable = Boolean.TRUE.equals(integerSchema.getNullable());

        if ("int64".equals(integerSchema.getFormat())) {
            return isNullable || Boolean.FALSE.equals(isRequired)
                    ? new ClassOrInterfaceTypeExpr("java.lang.Long")
                    : new PrimitiveTypeExpr(TypeKind.LONG);
        } else {
            return isNullable || Boolean.FALSE.equals(isRequired)
                    ? new ClassOrInterfaceTypeExpr("java.lang.Integer")
                    : new PrimitiveTypeExpr(TypeKind.INT);
        }
    }

    private TypeExpr createTypeDefault(final OpenAPI openAPI,
                                       final Schema<?> schema,
                                       final Map<String, Object> extensions,
                                       final String packageName,
                                       final String mediaType,
                                       final Boolean isRequired) {
        final var resolved = SchemaResolver.resolve(openAPI, schema);
        final var resolvedSchema = resolved.schema();

        return switch (resolvedSchema) {
            case null -> throw new NullPointerException("Resolved schema is null");
            case ObjectSchema ignored -> createObjectOrComposedType(openAPI, resolved, extensions, packageName);
            case ComposedSchema ignored -> createObjectOrComposedType(openAPI, resolved, extensions, packageName);
            default -> {
                if (resolvedSchema.getClass() == Schema.class) {
                    throw new UnsupportedOperationException();
                } else {
                    yield createType(
                            openAPI,
                            resolved.schema(),
                            extensions,
                            packageName,
                            mediaType,
                            isRequired
                    );
                }
            }
        };
    }

    private TypeExpr createObjectOrComposedType(final OpenAPI openAPI,
                                                final ResolvedSchemaResult resolved,
                                                final Map<String, Object> extensions,
                                                final String packageName) {
        final var name = resolved.name();
        if (name != null) {
            final var type = new ClassOrInterfaceTypeExpr(packageName + "." + resolved.name());
            processExtensions(openAPI, type, packageName, extensions);
            return type;
        } else {
            return new ClassOrInterfaceTypeExpr("java.lang.Object");
        }
    }

    private void processExtensions(final OpenAPI openAPI,
                                   final ClassOrInterfaceTypeExpr type,
                                   final String packageName,
                                   final Map<String, Object> extensions) {
        final var typeArgs = ExtensionsHelper.getExtension(extensions, "x-type-args", List.class);

        if (typeArgs != null) {
            for (final Object typeArg : typeArgs) {
                if (typeArg instanceof String className) {
                    final var typeArgType = new ClassOrInterfaceTypeExpr(className);
                    type.typeArgument(typeArgType);
                } else if (typeArg instanceof Map<?, ?> map) {
                    final var ref = (String) map.get("$ref");

                    if (ref != null) {
                        final var resolvedSchema = SchemaResolver.resolve(openAPI, ref);

                        final var typeArgType = createType(
                                openAPI,
                                resolvedSchema.schema(),
                                null,
                                packageName,
                                null,
                                true
                        );

                        if (typeArgType instanceof ClassOrInterfaceTypeExpr classOrInterfaceTypeExpr) {
                            classOrInterfaceTypeExpr.name(packageName + "." + resolvedSchema.name());
                        }

                        type.typeArgument(typeArgType);
                    }
                }
            }
        }
    }

    private TypeExpr createArrayType(final OpenAPI openAPI,
                                     final ArraySchema arraySchema,
                                     final String packageName,
                                     final String mediaType) {
        final var componentType = createType(
                openAPI,
                arraySchema.getItems(),
                null,
                packageName,
                mediaType,
                false
        );

        if (componentType instanceof PrimitiveTypeExpr primitiveType) {
            return new ArrayTypeExpr(primitiveType);
        } else {
            return new ClassOrInterfaceTypeExpr("java.util.List")
                    .typeArgument(componentType);
        }
    }

    private ClassOrInterfaceTypeExpr createMapType(final OpenAPI openAPI,
                                                   final MapSchema mapSchema,
                                                   final String packageName,
                                                   final String mediaType,
                                                   final Boolean isRequired) {
        final var keyType = new ClassOrInterfaceTypeExpr("java.lang.String");
        final var valueType = createType(
                openAPI,
                (Schema<?>) mapSchema.getAdditionalProperties(),
                null,
                packageName,
                mediaType,
                isRequired
        );

        return new ClassOrInterfaceTypeExpr("java.util.Map")
                .typeArguments(
                        keyType,
                        valueType
                );
    }

    @Override
    public TypeExpr asNonNull(final TypeExpr typeExpr) {
        if (typeExpr instanceof PrimitiveTypeExpr) {
            return typeExpr;
        } else if (typeExpr instanceof ClassOrInterfaceTypeExpr classOrInterfaceTypeExpr) {
            final var name = classOrInterfaceTypeExpr.getName();

            return switch (name) {
                case "java.lang.Boolean":
                    yield new ClassOrInterfaceTypeExpr("java.lang.Boolean");
                case "java.lang.Integer":
                    yield new ClassOrInterfaceTypeExpr("java.lang.Integer");
                case "java.lang.Long":
                    yield new ClassOrInterfaceTypeExpr("java.lang.Long");
                case "java.lang.Float":
                    yield new ClassOrInterfaceTypeExpr("java.lang.Float");
                case "java.lang.Double":
                    yield new ClassOrInterfaceTypeExpr("java.lang.Double");
                default:
                    yield typeExpr;
            };
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public TypeExpr createMultipartTypeExpression(final OpenAPI api) {
        return new ClassOrInterfaceTypeExpr("org.springframework.web.multipart.MultipartFile");
    }

    @Override
    public TypeExpr resolveImplementationType(final OpenAPI openAPI, final TypeExpr type) {
        final TypeExpr implementationType;

        if (type instanceof WildCardTypeExpr wildcardType) {
            implementationType = (TypeExpr) wildcardType.getExpr();
        } else {
            implementationType = type;
        }
        return implementationType;
    }

}
