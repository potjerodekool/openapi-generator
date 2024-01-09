package io.github.potjerodekool.openapi.internal.type;

import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.TypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.WildCardTypeExpression;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.*;

import java.util.List;
import java.util.Map;

import static io.github.potjerodekool.openapi.internal.util.OpenApiUtils.findComponentSchemaByName;
import static io.github.potjerodekool.openapi.internal.util.OpenApiUtils.getComponentSchemas;

public abstract class AbstractOpenApiTypeUtils implements OpenApiTypeUtils {

    private final OpenApiTypeUtils delegate;

    public AbstractOpenApiTypeUtils() {
        this.delegate = null;
    }

    public AbstractOpenApiTypeUtils(final OpenApiTypeUtils delegate) {
        this.delegate = delegate;
    }

    protected String resolveName(final OpenAPI openAPI,
                                 final ObjectSchema schema) {
        String name = schema.getName();

        if (name == null) {
            name = getComponentSchemas(openAPI).entrySet().stream()
                    .filter(entry -> schema == entry.getValue())
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(null);
        }

        return name;
    }

    @Override
    public TypeExpression createTypeExpression(final Schema<?> schema,
                                               final OpenAPI openAPI) {
        return createTypeExpression("", schema, openAPI);
    }

    @Override
    public TypeExpression createTypeExpression(final String mediaType,
                                               final Schema<?> schema,
                                               final OpenAPI openAPI) {
        if (schema instanceof StringSchema
                || schema instanceof PasswordSchema) {
            return createStringTypeExpression(schema, openAPI);
        } else if (schema instanceof IntegerSchema openApiIntegerSchema) {
            return createIntegerTypeExpression(openApiIntegerSchema, openAPI);
        } else if (schema instanceof BooleanSchema openApiBooleanSchema) {
            return createBooleanTypeExpression(openApiBooleanSchema, openAPI);
        } else if (schema instanceof NumberSchema openApiNumberSchema) {
            return createNumberTypeExpression(openApiNumberSchema, openAPI);
        } else if (schema instanceof DateSchema openApiDateSchema) {
            return createDateTypeExpression(openApiDateSchema, openAPI);
        } else if (schema instanceof ArraySchema openApiArraySchema) {
            return createArrayTypeExpression(openApiArraySchema, openAPI);
        } else if (schema instanceof ObjectSchema openApiObjectSchema) {
            return createObjectTypeExpression(openApiObjectSchema, openAPI);
        } else if (schema instanceof BinarySchema openApiBinarySchema) {
            return createBinaryTypeExpression(openApiBinarySchema, openAPI);
        } else if (schema instanceof MapSchema openApiMapSchema) {
            return createMapTypeExpression(openApiMapSchema, openAPI);
        } else if (schema instanceof UUIDSchema openApiUUIDSchema) {
            return createUUIDTypeExpression(openApiUUIDSchema, openAPI);
        } else if (schema instanceof DateTimeSchema openApiDateTimeSchema) {
            return createDateTimeExpression(openApiDateTimeSchema, openAPI);
        } else if (isImageOrVideo(mediaType)) {
            return createMultipartTypeExpression(openAPI);
        } else if (schema.get$ref() != null) {
            final String ref = schema.get$ref();
            final var resolvedSchema = findComponentSchemaByName(openAPI, ref);

            if (resolvedSchema == null) {
                throw new IllegalStateException(String.format("Failed to find schema %s", ref));
            } else {
                return createTypeExpression(mediaType, resolvedSchema, openAPI);
            }
        }

        throw new UnsupportedOperationException("" + schema);
    }

    @Override
    public ClassOrInterfaceTypeExpression createClassOrInterfaceTypeExpression(final String name,
                                                                               final boolean isNullable,
                                                                               final OpenAPI openAPI) {
        final var expression = new ClassOrInterfaceTypeExpression(name);
        expression.setNullable(isNullable);
        return expression;
    }

    @Override
    public ClassOrInterfaceTypeExpression createClassOrInterfaceTypeExpression(final String name,
                                                                               final Schema<?> schema,
                                                                               final OpenAPI openAPI) {
        final var isNullable = Boolean.TRUE.equals(schema.getNullable());
        return createClassOrInterfaceTypeExpression(name, isNullable, openAPI);
    }

    @Override
    public ClassOrInterfaceTypeExpression createClassOrInterfaceTypeExpression(final String name,
                                                                               final List<TypeExpression> typeArguments,
                                                                               final boolean isNullable,
                                                                               final OpenAPI openAPI) {
        final var paramType = new ClassOrInterfaceTypeExpression(name, typeArguments);
        paramType.setNullable(isNullable);
        return paramType;
    }

    @Override
    public TypeExpression resolveImplementationType(final TypeExpression type,
                                                    final OpenAPI openAPI) {
        final TypeExpression implementationType;

        if (type instanceof WildCardTypeExpression wildcardType) {
            implementationType = wildcardType.getTypeExpression();
        } else {
            implementationType = type;
        }
        return implementationType;
    }

    private boolean isImageOrVideo(final String mediaType) {
        return mediaType != null
                && (mediaType.startsWith("image/")
                || mediaType.startsWith("video/"));
    }

    @Override
    public TypeExpression createObjectTypeExpression(final ObjectSchema schema, final OpenAPI openAPI) {
        return delegate != null
                ? delegate.createObjectTypeExpression(schema, openAPI)
                : null;
    }

    @Override
    public TypeExpression createStringTypeExpression(final Schema<?> schema,
                                                     final OpenAPI openAPI) {
        return delegate != null
                ? delegate.createStringTypeExpression(schema, openAPI)
                : null;
    }

    @Override
    public TypeExpression createUUIDTypeExpression(final UUIDSchema schema,
                                                   final OpenAPI openAPI) {
        return delegate != null
                ? delegate.createUUIDTypeExpression(schema, openAPI)
                : null;
    }

    @Override
    public TypeExpression createDateTimeExpression(final DateTimeSchema openApiDateTimeSchema,
                                                   final OpenAPI openAPI) {
        return delegate != null
                ? delegate.createDateTimeExpression(openApiDateTimeSchema, openAPI)
                : null;
    }

    @Override
    public TypeExpression createIntegerTypeExpression(final IntegerSchema schema,
                                                      final OpenAPI openAPI) {
        return delegate != null
                ? delegate.createIntegerTypeExpression(schema, openAPI)
                : null;
    }

    @Override
    public TypeExpression createBooleanTypeExpression(final BooleanSchema schema,
                                                      final OpenAPI openAPI) {
        return delegate != null
                ? delegate.createBooleanTypeExpression(schema, openAPI)
                : null;
    }

    @Override
    public TypeExpression createNumberTypeExpression(final NumberSchema schema,
                                                     final OpenAPI openAPI) {
        return delegate != null
                ? delegate.createNumberTypeExpression(schema, openAPI)
                : null;
    }

    @Override
    public TypeExpression createDateTypeExpression(final DateSchema schema,
                                                   final OpenAPI openAPI) {
        return delegate != null
                ? delegate.createDateTypeExpression(schema, openAPI)
                : null;
    }

    @Override
    public TypeExpression createArrayTypeExpression(final ArraySchema schema,
                                                    final OpenAPI openAPI) {
        return delegate != null
                ? delegate.createArrayTypeExpression(schema, openAPI)
                : null;
    }

    @Override
    public TypeExpression createBinaryTypeExpression(final BinarySchema schema,
                                                     final OpenAPI openAPI) {
        return delegate != null
                ? delegate.createBinaryTypeExpression(schema, openAPI)
                : null;
    }

    @Override
    public TypeExpression createMapTypeExpression(final MapSchema schema,
                                                  final OpenAPI openAPI) {
        return delegate != null
                ? delegate.createMapTypeExpression(schema, openAPI)
                : null;
    }

    @Override
    public ClassOrInterfaceTypeExpression createMultipartTypeExpression(final OpenAPI openAPI) {
        return delegate != null
                ? delegate.createMultipartTypeExpression(openAPI)
                : null;
    }
}
