package io.github.potjerodekool.openapi.codegen.modelcodegen.builder;

import io.github.potjerodekool.openapi.codegen.HttpMethod;
import io.github.potjerodekool.openapi.codegen.SchemaResolver;
import io.github.potjerodekool.openapi.codegen.modelcodegen.model.element.Model;
import io.github.potjerodekool.openapi.codegen.modelcodegen.model.element.ModelProperty;
import io.github.potjerodekool.openapi.codegen.modelcodegen.model.type.PrimitiveType;
import io.github.potjerodekool.openapi.codegen.modelcodegen.model.type.ReferenceType;
import io.github.potjerodekool.openapi.codegen.modelcodegen.model.type.Type;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.*;

public class JavaModelBuilder implements ModelBuilder {
    @Override
    public Model build(final OpenAPI openAPI,
                       final HttpMethod httpMethod,
                       final String name,
                       final Schema<?> schema) {
        final var model = new Model();
        model.simpleName(name);
        schema.getProperties().forEach((propertyName, propertySchema) -> addProperty(openAPI, httpMethod, propertyName, propertySchema, model));

        return model;
    }

    private void addProperty(final OpenAPI openAPI,
                             final HttpMethod httpMethod,
                             final String propertyName,
                             final Schema<?> propertySchema, final Model model) {
        final var resolvedPropertySchema = SchemaResolver.resolve(openAPI, propertySchema)
                .schema();

        if (resolvedPropertySchema != null) {
            final var isNullable = httpMethod == HttpMethod.PATCH
                    ? true
                    : null;

            var type = resolveType(resolvedPropertySchema, isNullable);

            if (httpMethod == HttpMethod.PATCH) {
                type = new ReferenceType()
                        .name("org.openapitools.jackson.nullable.JsonNullable")
                        .typeArg(type);
            }

            final var property = new ModelProperty()
                    .simpleName(propertyName)
                    .type(type);
            model.enclosedElement(property);
        }
    }

    private Type resolveType(final Schema<?> schema, final Boolean isNullable) {
        return switch (schema) {
            case IntegerSchema integerSchema -> resolveIntegerType(integerSchema, isNullable);
            case StringSchema stringSchema -> resolveStringType(stringSchema);
            case PasswordSchema passwordSchema -> resolveStringType(passwordSchema);
            case DateSchema dateSchema -> resolveDateType(dateSchema);
            default -> throw new UnsupportedOperationException("resolveType " + schema);
        };
    }

    private Type resolveIntegerType(final IntegerSchema schema,
                                    final Boolean isNullable) {
        final var format = schema.getFormat();

        if (Boolean.TRUE.equals(schema.getNullable()) || Boolean.TRUE.equals(isNullable)) {
            return "int64".equals(format)
                    ? new ReferenceType().name("java.lang.Long")
                    : new ReferenceType().name("java.lang.Integer");
        } else {
            return "int64".equals(format)
                    ? new PrimitiveType().name("long")
                    : new PrimitiveType().name("int");
        }
    }

    private Type resolveStringType(final Schema<?> stringSchema) {
        return new ReferenceType().name("java.lang.String");
    }

    private Type resolveDateType(final DateSchema dateSchema) {
        return new ReferenceType().name("java.time.LocalDate");
    }
}
