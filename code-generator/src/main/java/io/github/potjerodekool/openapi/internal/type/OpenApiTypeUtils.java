    package io.github.potjerodekool.openapi.internal.type;

import io.github.potjerodekool.codegen.model.tree.type.*;
import io.github.potjerodekool.codegen.model.util.StringUtils;
import io.github.potjerodekool.openapi.internal.ClassNames;
import io.github.potjerodekool.openapi.tree.media.*;

import java.util.List;

public abstract class OpenApiTypeUtils {

    public TypeExpression createTypeExpression(final OpenApiSchema<?> schema) {
        return createTypeExpression("", schema);
    }

    public TypeExpression createTypeExpression(final String mediaType,
                                               final OpenApiSchema<?> schema) {
        if (schema instanceof OpenApiStringSchema
            || schema instanceof OpenApiPasswordSchema) {
            return createStringTypeExpression(schema);
        } else if (schema instanceof OpenApiIntegerSchema openApiIntegerSchema) {
            return createIntegerTypeExpression(openApiIntegerSchema);
        } else if (schema instanceof OpenApiBooleanSchema openApiBooleanSchema) {
            return createBooleanTypeExpression(openApiBooleanSchema);
        } else if (schema instanceof OpenApiNumberSchema openApiNumberSchema) {
            return createNumberTypeExpression(openApiNumberSchema);
        } else if (schema instanceof OpenApiDateSchema openApiDateSchema) {
            return createDateTypeExpression(openApiDateSchema);
        } else if (schema instanceof OpenApiArraySchema openApiArraySchema) {
            return createArrayTypeExpression(openApiArraySchema);
        } else if (schema instanceof OpenApiObjectSchema openApiObjectSchema) {
            return createObjectTypeExpression(openApiObjectSchema);
        } else if (schema instanceof OpenApiBinarySchema openApiBinarySchema) {
            return createBinaryTypeExpression(openApiBinarySchema);
        } else if (schema instanceof OpenApiMapSchema openApiMapSchema) {
            return createMapTypeExpression(openApiMapSchema);
        } else if (schema instanceof OpenApiUUIDSchema openApiUUIDSchema) {
            return createUUIDTypeExpression(openApiUUIDSchema);
        } else if (schema instanceof OpenApiDateTimeSchema openApiDateTimeSchema) {
            return createDateTimeExpression(openApiDateTimeSchema);
        } else if (isImageOrVideo(mediaType)) {
            return createMultipartTypeExpression();
        }

        throw new UnsupportedOperationException("" + schema);
    }

    private boolean isImageOrVideo(final String mediaType) {
        return mediaType != null
                && (mediaType.startsWith("image/")
                    || mediaType.startsWith("video/"));
    }

    private TypeExpression createObjectTypeExpression(final OpenApiObjectSchema schema) {
        final String className;

        if ("object".equals(schema.name()) && schema.pck().isUnnamed()) {
            className = "java.lang.Object";
        } else {
            final var name = StringUtils.firstUpper(schema.name());
            final var pck = schema.pck();
            className = pck.isUnnamed()
                    ? name
                    : pck.getName() + "." + name;
        }

        return createClassOrInterfaceTypeExpression(className, schema);
    }

    protected TypeExpression createStringTypeExpression(final OpenApiSchema<?> schema) {
        return createClassOrInterfaceTypeExpression("java.lang.String", schema);
    }

    private TypeExpression createUUIDTypeExpression(final OpenApiUUIDSchema schema) {
        return createClassOrInterfaceTypeExpression("java.util.UUID", schema);
    }

    private TypeExpression createDateTimeExpression(final OpenApiDateTimeSchema openApiDateTimeSchema) {
        return createClassOrInterfaceTypeExpression( "java.time.OffsetDateTime", openApiDateTimeSchema);
    }

    private TypeExpression createIntegerTypeExpression(final OpenApiIntegerSchema schema) {
        final var isNullable = Boolean.TRUE.equals(schema.nullable());

        if ("int64".equals(schema.format())) {
            return isNullable
                    ? createClassOrInterfaceTypeExpression( "java.lang.Long", true)
                    : PrimitiveTypeExpression.longTypeExpression();
        } else {
            return isNullable
                    ? createClassOrInterfaceTypeExpression( "java.lang.Integer", true)
                    : PrimitiveTypeExpression.intTypeExpression();
        }
    }

    private TypeExpression createBooleanTypeExpression(final OpenApiBooleanSchema schema) {
        final var isNullable = Boolean.TRUE.equals(schema.nullable());
        return isNullable
                ? createClassOrInterfaceTypeExpression( "java.lang.Boolean", true)
                : PrimitiveTypeExpression.booleanTypeExpression();
    }

    private TypeExpression createNumberTypeExpression(final OpenApiNumberSchema schema) {
        final var isNullable = Boolean.TRUE.equals(schema.nullable());

        if ("double".equals(schema.format())) {
            return isNullable
                    ? createClassOrInterfaceTypeExpression( "java.lang.Double", true)
                    : PrimitiveTypeExpression.doubleTypeExpression();
        } else  {
            return isNullable
                    ? createClassOrInterfaceTypeExpression( "java.lang.Float", true)
                    : PrimitiveTypeExpression.floatTypeExpression();
        }
    }

    private TypeExpression createDateTypeExpression(final OpenApiDateSchema schema) {
        return createClassOrInterfaceTypeExpression("java.time.LocalDate", schema);
    }

    private TypeExpression createArrayTypeExpression(final OpenApiArraySchema schema) {
        final var componentType = createTypeExpression(schema.itemschema());

        if (componentType instanceof PrimitiveTypeExpression) {
            return new ArrayTypeExpression(componentType);
        }

        return createClassOrInterfaceTypeExpression(ClassNames.LIST_CLASS_NAME,
                List.of(componentType),
                !Boolean.FALSE.equals(schema.nullable())
        );
    }

    protected abstract TypeExpression createBinaryTypeExpression(final OpenApiBinarySchema schema);

    protected ClassOrInterfaceTypeExpression createClassOrInterfaceTypeExpression(final String name,
                                                                                  final boolean isNullable) {
        final var expression = new ClassOrInterfaceTypeExpression(name);
        expression.setNullable(isNullable);
        return expression;
    }

    protected ClassOrInterfaceTypeExpression createClassOrInterfaceTypeExpression(final String name,
                                                                                  final OpenApiSchema<?> schema) {
        final var isNullable = Boolean.TRUE.equals(schema.nullable());
        return createClassOrInterfaceTypeExpression(name, isNullable);
    }

    private ClassOrInterfaceTypeExpression createClassOrInterfaceTypeExpression(final String name,
                                                                                final List<TypeExpression> typeArguments,
                                                                                final boolean isNullable) {
        final var paramType = new ClassOrInterfaceTypeExpression(name,  typeArguments);
        paramType.setNullable(isNullable);
        return paramType;
    }

    private TypeExpression createMapTypeExpression(final OpenApiMapSchema schema) {
        final var additionalProperties = schema.additionalProperties();

        if (additionalProperties == null) {
            throw new IllegalArgumentException(String.format("schema %s has no additionalProperties", schema.name()));
        }
        final var keyType = createStringTypeExpression(Boolean.TRUE.equals(schema.nullable()));
        final var valueType = createTypeExpression(additionalProperties);

        return createClassOrInterfaceTypeExpression(
                "java.util.Map",
                List.of(keyType, valueType),
                Boolean.TRUE.equals(schema.nullable())
        );
    }

    private ClassOrInterfaceTypeExpression createStringTypeExpression(final boolean isNullable) {
        return createClassOrInterfaceTypeExpression("java.lang.String", isNullable);
    }

    public abstract ClassOrInterfaceTypeExpression createMultipartTypeExpression();

    public TypeExpression resolveImplementationType(final TypeExpression type) {
        final TypeExpression implementationType;

        if (type instanceof WildCardTypeExpression wildcardType) {
            implementationType = wildcardType.getTypeExpression();
        } else {
            implementationType = type;
        }
        return implementationType;
    }
}
