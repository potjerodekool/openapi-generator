package io.github.potjerodekool.openapi.internal.type;

import io.github.potjerodekool.codegen.model.tree.type.ArrayTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.PrimitiveTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.TypeExpression;
import io.github.potjerodekool.codegen.model.util.StringUtils;
import io.github.potjerodekool.openapi.internal.ClassNames;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.*;

import java.util.List;

public class OpenApiTypeUtilsJava extends AbstractOpenApiTypeUtils {

    private final String modelPackageName;

    public OpenApiTypeUtilsJava(final String modelPackageName) {
        this.modelPackageName = modelPackageName;
    }

    @Override
    public TypeExpression createObjectTypeExpression(final ObjectSchema schema,
                                                     final OpenAPI openAPI) {
        final String name = resolveName(openAPI, schema);
        final String className;

        if ("object".equals(schema.getName()) || name == null) {
            className = "java.lang.Object";
        } else {
            final var simpleName = StringUtils.firstUpper(name);
            className = modelPackageName + "." + simpleName;
        }

        return createClassOrInterfaceTypeExpression(className, schema, openAPI);
    }

    @Override
    public TypeExpression createStringTypeExpression(final Schema<?> schema,
                                                     final OpenAPI openAPI) {
        return createClassOrInterfaceTypeExpression("java.lang.String", schema, openAPI);
    }

    @Override
    public TypeExpression createUUIDTypeExpression(final UUIDSchema schema,
                                                   final OpenAPI openAPI) {
        return createClassOrInterfaceTypeExpression("java.util.UUID", schema, openAPI);
    }

    @Override
    public TypeExpression createDateTimeExpression(final DateTimeSchema openApiDateTimeSchema,
                                                   final OpenAPI openAPI) {
        return createClassOrInterfaceTypeExpression("java.time.OffsetDateTime", openApiDateTimeSchema, openAPI);
    }

    @Override
    public TypeExpression createIntegerTypeExpression(final IntegerSchema schema,
                                                      final OpenAPI openAPI) {
        final var isNullable = Boolean.TRUE.equals(schema.getNullable());

        if ("int64".equals(schema.getFormat())) {
            return isNullable
                    ? createClassOrInterfaceTypeExpression("java.lang.Long", true, openAPI)
                    : PrimitiveTypeExpression.longTypeExpression();
        } else {
            return isNullable
                    ? createClassOrInterfaceTypeExpression("java.lang.Integer", true, openAPI)
                    : PrimitiveTypeExpression.intTypeExpression();
        }
    }

    @Override
    public TypeExpression createBooleanTypeExpression(final BooleanSchema schema,
                                                      final OpenAPI openAPI) {
        final var isNullable = Boolean.TRUE.equals(schema.getNullable());
        return isNullable
                ? createClassOrInterfaceTypeExpression("java.lang.Boolean", true, openAPI)
                : PrimitiveTypeExpression.booleanTypeExpression();
    }

    @Override
    public TypeExpression createNumberTypeExpression(final NumberSchema schema,
                                                     final OpenAPI openAPI) {
        final var isNullable = Boolean.TRUE.equals(schema.getNullable());

        if ("double".equals(schema.getFormat())) {
            return isNullable
                    ? createClassOrInterfaceTypeExpression("java.lang.Double", true, openAPI)
                    : PrimitiveTypeExpression.doubleTypeExpression();
        } else {
            return isNullable
                    ? createClassOrInterfaceTypeExpression("java.lang.Float", true, openAPI)
                    : PrimitiveTypeExpression.floatTypeExpression();
        }
    }

    @Override
    public TypeExpression createDateTypeExpression(final DateSchema schema,
                                                   final OpenAPI openAPI) {
        return createClassOrInterfaceTypeExpression("java.time.LocalDate", schema, openAPI);
    }

    @Override
    public TypeExpression createArrayTypeExpression(final ArraySchema schema,
                                                    final OpenAPI openAPI) {
        final var componentType = createTypeExpression(schema.getItems(), openAPI);

        if (componentType instanceof PrimitiveTypeExpression) {
            return new ArrayTypeExpression(componentType);
        }

        return createClassOrInterfaceTypeExpression(ClassNames.LIST_CLASS_NAME,
                List.of(componentType),
                !Boolean.FALSE.equals(schema.getNullable()),
                openAPI
        );
    }

    @Override
    public TypeExpression createBinaryTypeExpression(final BinarySchema schema,
                                                     final OpenAPI openAPI) {
        return null;
    }

    @Override
    public TypeExpression createMapTypeExpression(final MapSchema schema,
                                                  final OpenAPI openAPI) {
        final var additionalProperties = (Schema<?>) schema.getAdditionalProperties();

        if (additionalProperties == null) {
            throw new IllegalArgumentException(String.format("schema %s has no additionalProperties", schema.getName()));
        }

        final var keyType = createStringTypeExpression(Boolean.TRUE.equals(schema.getNullable()), openAPI);
        final var valueType = createTypeExpression(additionalProperties, openAPI);

        return createClassOrInterfaceTypeExpression(
                "java.util.Map",
                List.of(keyType, valueType),
                Boolean.TRUE.equals(schema.getNullable()),
                openAPI
        );
    }

    @Override
    public ClassOrInterfaceTypeExpression createMultipartTypeExpression(final OpenAPI openAPI) {
        return null;
    }

    private ClassOrInterfaceTypeExpression createStringTypeExpression(final boolean isNullable,
                                                                      final OpenAPI openAPI) {
        return createClassOrInterfaceTypeExpression("java.lang.String", isNullable, openAPI);
    }
}
