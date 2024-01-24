package io.github.potjerodekool.openapi.internal.generate.incurbation;

import io.github.potjerodekool.codegen.template.model.type.ArrayTypeExpr;
import io.github.potjerodekool.codegen.template.model.type.ClassOrInterfaceTypeExpr;
import io.github.potjerodekool.codegen.template.model.type.PrimitiveTypeExpr;
import io.github.potjerodekool.codegen.template.model.type.TypeExpr;
import io.github.potjerodekool.openapi.internal.generate.model.SchemaResolver;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.*;

public class TypeUtilsSpringImpl implements TypeUtils {
    @Override
    public TypeExpr createType(final OpenAPI openAPI,
                               final Schema<?> schema,
                               final String packageName,
                               final String mediaType) {
        return switch (schema) {
            case BinarySchema ignored -> new ClassOrInterfaceTypeExpr("org.springframework.core.io.Resource");
            case ArraySchema arraySchema -> createArrayType(openAPI, arraySchema, packageName, mediaType);
            case MapSchema mapSchema -> createMapType(openAPI, mapSchema, packageName, mediaType);
            case IntegerSchema integerSchema -> createIntegerType(integerSchema);
            default -> createTypeDefault(openAPI, schema, packageName, mediaType);
        };
    }

    private TypeExpr createIntegerType(final IntegerSchema integerSchema) {
        final var isNullable = Boolean.TRUE.equals(integerSchema.getNullable());

        if ("int64".equals(integerSchema.getFormat())) {
            return isNullable
                    ? new ClassOrInterfaceTypeExpr("java.lang.Long")
                    : new PrimitiveTypeExpr("long");
        } else {
            return isNullable
                    ? new ClassOrInterfaceTypeExpr("java.lang.Integer")
                    : new PrimitiveTypeExpr("int");
        }
    }

    private TypeExpr createTypeDefault(final OpenAPI openAPI,
                                       final Schema<?> schema,
                                       final String packageName,
                                       final String mediaType) {
        final var resolved = SchemaResolver.resolve(openAPI, schema);

        if (resolved.schema() instanceof ObjectSchema) {
            final var name = resolved.name();
            if (name != null) {
                return new ClassOrInterfaceTypeExpr(packageName + "." + resolved.name());
            } else {
                return new ClassOrInterfaceTypeExpr("java.lang.Object");
            }
        } else if (resolved.schema().getClass() == Schema.class) {
            throw new UnsupportedOperationException();
        } else {
            return createType(
                    openAPI,
                    resolved.schema(),
                    packageName,
                    mediaType
            );
        }
    }

    private TypeExpr createArrayType(final OpenAPI openAPI,
                                     final ArraySchema arraySchema,
                                     final String packageName,
                                     final String mediaType) {
        final var componentType = createType(openAPI, arraySchema.getItems(), packageName, mediaType);

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
                                                   final String mediaType) {
        final var keyType = new ClassOrInterfaceTypeExpr("java.lang.String");
        final var valueType = createType(
                openAPI,
                (Schema<?>) mapSchema.getAdditionalProperties(),
                packageName,
                mediaType
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
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public TypeExpr createMultipartTypeExpression(final OpenAPI api) {
        return new ClassOrInterfaceTypeExpr("org.springframework.web.multipart.MultipartFile");
    }

}