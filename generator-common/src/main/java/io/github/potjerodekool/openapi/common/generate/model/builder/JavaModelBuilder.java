package io.github.potjerodekool.openapi.common.generate.model.builder;

import io.github.potjerodekool.openapi.common.generate.ExtensionsHelper;
import io.github.potjerodekool.openapi.common.generate.model.element.Model;
import io.github.potjerodekool.openapi.common.generate.model.type.TypeVariable;
import io.github.potjerodekool.openapi.common.generate.model.type.Type;
import io.github.potjerodekool.openapi.common.generate.SchemaResolver;
import io.github.potjerodekool.openapi.common.generate.model.element.ModelProperty;
import io.github.potjerodekool.openapi.common.generate.model.type.PrimitiveType;
import io.github.potjerodekool.openapi.common.generate.model.type.ReferenceType;
import io.github.potjerodekool.openapi.common.generate.model.type.WildCardType;
import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.*;

import java.util.*;

public class JavaModelBuilder {

    private final String modelPackageName;

    public JavaModelBuilder(final String modelPackageName) {
        this.modelPackageName = modelPackageName;
    }

    public Model build(final OpenAPI openAPI,
                       final HttpMethod httpMethod,
                       final String packageName,
                       final String name,
                       final Schema<?> schema) {
        final var model = new Model();
        model.simpleName(name);
        model.packageName(packageName);
        final var processedProperties = new HashSet<String>();
        addProperties(openAPI, httpMethod, schema, model, processedProperties);

        processModelExtensions(schema, model);
        return model;
    }

    private void processModelExtensions(final Schema<?> resolvedSchema, final Model model) {
        final List<String> typeArgs = ExtensionsHelper.getExtension(resolvedSchema.getExtensions(), "x-type-args", List.class);

        if (!typeArgs.isEmpty()) {
            final var typeArguments = new ArrayList<TypeVariable>();
            final var className = model.getPackageName() + "." + model.getSimpleName();

            final var selfTypeArgs = new ArrayList<Type>();
            selfTypeArgs.add(new TypeVariable().name("SELF"));
            selfTypeArgs.addAll(typeArgs.stream()
                    .map(name -> new TypeVariable().name(name))
                    .toList());

            typeArguments.add(new TypeVariable()
                    .name("SELF")
                    .bounds(new WildCardType().extendsBound(
                            new ReferenceType()
                                    .name(className)
                                    .typeArgs(selfTypeArgs)
                    ))
            );

            typeArguments.addAll(
                    typeArgs.stream()
                            .map(name -> new TypeVariable().name(name))
                            .toList()
            );

            model.typeArguments(typeArguments);
        }
    }

    private void addProperties(final OpenAPI openAPI,
                               final HttpMethod httpMethod,
                               final Schema<?> schema,
                               final Model model, final HashSet<String> processedProperties) {
        if (schema.getAllOf() != null) {
            Schema<?> ignoreSchema;

            if (schema.getAllOf().size() == 1) {
                ignoreSchema = SchemaResolver.resolve(openAPI, schema.getAllOf().getFirst()).schema();
                collectPropertyNames(ignoreSchema, processedProperties);

                final var parentType = resolveType(schema.getAllOf().getFirst(), httpMethod == HttpMethod.PATCH, openAPI);

                if (parentType instanceof ReferenceType referenceType) {
                    model.superType(referenceType);
                    final List<Map<String, String>> typeArgs = ExtensionsHelper.getExtension(
                            schema.getExtensions(),
                            "x-super-type-args",
                            List.class
                    );

                    if (!typeArgs.isEmpty()) {
                        referenceType.typeArg(
                                new ReferenceType()
                                        .name(model.getPackageName() + "." + model.getSimpleName())
                        );

                        for (final Map<String, String> typeArg : typeArgs) {
                            final var ref = typeArg.get("$ref");

                            if (ref != null) {
                                final var resolved = SchemaResolver.resolve(
                                        openAPI,
                                        ref
                                );

                                if (resolved.schema() != null) {
                                    final var type = (ReferenceType) resolveType(resolved.schema(), httpMethod == HttpMethod.PATCH, openAPI);
                                    type.name(resolved.name());
                                    referenceType.typeArg(type);
                                }
                            }
                        }
                    }
                }
            }

            for (final Schema<?> otherSchema : schema.getAllOf()) {
                final var resolved = SchemaResolver.resolve(openAPI, otherSchema);

                if (resolved.schema() != null) {
                    addProperties(openAPI, httpMethod, resolved.schema(), model, processedProperties);
                }
            }
        }

        if (schema.getProperties() != null) {
            schema.getProperties()
                    .forEach((propertyName, propertySchema) ->
                            addProperty(
                                    openAPI,
                                    httpMethod,
                                    propertyName,
                                    propertySchema,
                                    model,
                                    processedProperties
                            ));
        }
    }

    private void collectPropertyNames(final Schema<?> schema,
                                      final HashSet<String> processedProperties) {
        final var properties = schema.getProperties();

        if (properties != null) {
            processedProperties.addAll(properties.keySet());
        }
    }


    private void addProperty(final OpenAPI openAPI,
                             final HttpMethod httpMethod,
                             final String propertyName,
                             final Schema<?> propertySchema, final Model model, final HashSet<String> processedProperties) {
        if (model.getProperty(propertyName).isPresent()
                || processedProperties.contains(propertyName)) {
            return;
        }

        final var resolvedPropertySchema = SchemaResolver.resolve(openAPI, propertySchema)
                .schema();

        if (resolvedPropertySchema != null) {
            final var isPatch = httpMethod == HttpMethod.PATCH
                    ? true
                    : null;

            var type = resolveType(resolvedPropertySchema, isPatch, openAPI);

            if (httpMethod == HttpMethod.PATCH) {
                type = new ReferenceType()
                        .name("org.openapitools.jackson.nullable.JsonNullable")
                        .typeArg(type);
            }

            final var property = new ModelProperty()
                    .simpleName(propertyName)
                    .type(type);
            model.enclosedElement(property);
            processedProperties.add(propertyName);
        }
    }

    private Type resolveType(final Schema<?> schema,
                             final Boolean isPatch,
                             final OpenAPI openAPI) {
        return switch (schema) {
            case IntegerSchema integerSchema -> resolveIntegerType(integerSchema, isPatch);
            case NumberSchema ignored -> new ReferenceType().name("java.math.BigDecimal");
            case StringSchema ignored -> resolveStringType();
            case EmailSchema ignored -> resolveStringType();
            case PasswordSchema ignored -> resolveStringType();
            case DateSchema ignored -> resolveDateType();
            case DateTimeSchema ignored -> new ReferenceType().name("java.time.OffsetDateTime");
            case MapSchema ignored -> new ReferenceType().name("java.util.Map");
            case ArraySchema arraySchema -> resolveListType(arraySchema, openAPI, isPatch);
            case UUIDSchema ignored -> new ReferenceType().name("java.util.UUID");
            case BooleanSchema booleanSchema -> resolveBooleanType(booleanSchema);
            case ObjectSchema objectSchema -> {
                final var typeArg = ExtensionsHelper.getExtension(
                        objectSchema.getExtensions(),
                        "x-type-arg",
                        String.class
                );

                if (typeArg == null) {
                    yield new ReferenceType().name("java.lang.Object");
                } else {
                    yield new ReferenceType().name(typeArg);
                }
            }
            default -> {
                if (schema.get$ref() != null) {
                    final var result = SchemaResolver.resolve(openAPI, schema);
                    if (result.schema() instanceof ObjectSchema) {
                        yield new ReferenceType().name(this.modelPackageName + "." + result.name());
                    }
                }

                throw new UnsupportedOperationException("resolveType " + schema);
            }
        };
    }

    private Type resolveIntegerType(final IntegerSchema schema,
                                    final Boolean isPatch) {
        final var format = schema.getFormat();

        if (Boolean.TRUE.equals(schema.getNullable()) || Boolean.TRUE.equals(isPatch)) {
            return "int64".equals(format)
                    ? new ReferenceType().name("java.lang.Long")
                    : new ReferenceType().name("java.lang.Integer");
        } else {
            return "int64".equals(format)
                    ? new PrimitiveType().name("long")
                    : new PrimitiveType().name("int");
        }
    }

    private Type resolveStringType() {
        return new ReferenceType().name("java.lang.String");
    }

    private Type resolveDateType() {
        return new ReferenceType().name("java.time.LocalDate");
    }

    private Type resolveBooleanType(final BooleanSchema booleanSchema) {
        return Boolean.FALSE.equals(booleanSchema.getNullable())
                ? new PrimitiveType().name("boolean")
                : new ReferenceType().name("java.lang.Boolean");
    }

    private Type resolveListType(final ArraySchema arraySchema,
                                 final OpenAPI openAPI,
                                 final Boolean isPatch) {
        final var listType = new ReferenceType().name("java.util.List");

        final var componentType = resolveType(
                arraySchema.getItems(),
                isPatch,
                openAPI
        );

        listType.typeArg(componentType);

        return listType;
    }
}
