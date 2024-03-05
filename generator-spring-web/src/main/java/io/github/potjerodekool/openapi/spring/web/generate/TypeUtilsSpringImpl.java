package io.github.potjerodekool.openapi.spring.web.generate;

import io.github.potjerodekool.codegen.model.type.TypeKind;
import io.github.potjerodekool.codegen.template.model.type.*;
import io.github.potjerodekool.openapi.common.generate.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.common.generate.ResolvedSchemaResult;
import io.github.potjerodekool.openapi.common.generate.SchemaResolver;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.*;

public class TypeUtilsSpringImpl implements OpenApiTypeUtils {
    @Override
    public TypeExpr createType(final OpenAPI openAPI,
                               final Schema<?> schema,
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
            case ObjectSchema ignored -> new ClassOrInterfaceTypeExpr("java.lang.Object");
            case PasswordSchema ignored -> createStringTypeExpr();
            case StringSchema ignored -> createStringTypeExpr();
            case UUIDSchema ignored -> createUuidTypeExpr();
            default -> createTypeDefault(openAPI, schema, packageName, mediaType, isRequired);
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
                                       final String packageName,
                                       final String mediaType,
                                       final Boolean isRequired) {
        final var resolved = SchemaResolver.resolve(openAPI, schema);
        final var resolvedSchema = resolved.schema();

        return switch (resolvedSchema) {
            case null -> throw new NullPointerException("Resolved schema is null");
            case ObjectSchema ignored -> createObjectOrComposedType(resolved, packageName);
            case ComposedSchema ignored -> createObjectOrComposedType(resolved, packageName);
            default -> {
                if (resolvedSchema.getClass() == Schema.class) {
                    throw new UnsupportedOperationException();
                } else {
                    yield createType(
                            openAPI,
                            resolved.schema(),
                            packageName,
                            mediaType,
                            isRequired
                    );
                }
            }
        };
    }

    private TypeExpr createObjectOrComposedType(final ResolvedSchemaResult resolved, final String packageName){
        final var name = resolved.name();
        if (name != null) {
            return new ClassOrInterfaceTypeExpr(packageName + "." + resolved.name());
        } else {
            return new ClassOrInterfaceTypeExpr("java.lang.Object");
        }
    }

    private TypeExpr createArrayType(final OpenAPI openAPI,
                                     final ArraySchema arraySchema,
                                     final String packageName,
                                     final String mediaType) {
        final var componentType = createType(
                openAPI,
                arraySchema.getItems(),
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