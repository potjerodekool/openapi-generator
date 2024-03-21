package io.github.potjerodekool.openapi.common.generate.model.builder;

import io.github.potjerodekool.codegen.model.tree.type.BoundKind;
import io.github.potjerodekool.codegen.model.type.TypeKind;
import io.github.potjerodekool.codegen.model.util.StringUtils;
import io.github.potjerodekool.codegen.template.model.type.*;
import io.github.potjerodekool.openapi.common.generate.Extensions;
import io.github.potjerodekool.openapi.common.generate.ExtensionsHelper;
import io.github.potjerodekool.openapi.common.generate.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.common.generate.SchemaResolver;
import io.github.potjerodekool.openapi.common.generate.model.element.Element;
import io.github.potjerodekool.openapi.common.generate.model.element.JavaModifier;
import io.github.potjerodekool.openapi.common.generate.model.element.Model;
import io.github.potjerodekool.openapi.common.generate.model.element.ModelProperty;
import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class JavaModelBuilder {

    private final String modelPackageName;
    private final OpenApiTypeUtils typeUtils;

    public JavaModelBuilder(final String modelPackageName,
                            final OpenApiTypeUtils typeUtils) {
        this.modelPackageName = modelPackageName;
        this.typeUtils = typeUtils;
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
        postProcessProperties(schema, model);
        processModelExtensions(schema, model);
        return model;
    }

    private void postProcessProperties(final Schema<?> schema, final Model model) {
        if (schema.getProperties() == null) {
            return;
        }

        schema.getProperties().entrySet().stream()
                .filter(entry -> entry.getValue() instanceof StringSchema)
                .filter(entry -> entry.getValue().getEnum() != null)
                .forEach(entry -> {
                    final var enumName = StringUtils.firstUpper(entry.getKey()) + "Enum";
                    final var enumElementOptional = model.getEnclosedElements().stream()
                            .filter(it -> it.getSimpleName().equals(enumName))
                            .findFirst();

                    if (enumElementOptional.isEmpty()) {
                        final var enumModel = new Model()
                                .simpleName(enumName)
                                .kind(Element.Kind.ENUM)
                                .modifiers(JavaModifier.PUBLIC);
                        final List<String> enumValues = entry.getValue().getEnum();
                        enumValues.forEach(enumValue -> enumModel.enclosedElement(
                                new ModelProperty()
                                        .simpleName(enumValue)
                                        .kind(Element.Kind.ENUM_CONSTANT)
                                        .type(new ClassOrInterfaceTypeExpr().simpleName(enumName))
                        ));
                        model.enclosedElement(enumModel);
                    }
                });
    }

    private void processModelExtensions(final Schema<?> resolvedSchema, final Model model) {
        final List<String> typeArgs = ExtensionsHelper.getExtension(resolvedSchema.getExtensions(), Extensions.TYPE_ARGS, List.class);

        if (!typeArgs.isEmpty()) {
            final var typeArguments = new ArrayList<TypeVarExpr>();

            final var selfTypeArgs = new ArrayList<TypeExpr>();
            selfTypeArgs.add(new TypeVarExpr().name("SELF"));
            selfTypeArgs.addAll(typeArgs.stream()
                    .map(name -> new TypeVarExpr().name(name))
                    .toList());

            typeArguments.add(new TypeVarExpr()
                    .name("SELF")
                    .bounds(new WildCardTypeExpr().expr(
                            new ClassOrInterfaceTypeExpr()
                                    .packageName(model.getPackageName())
                                    .simpleName(model.getSimpleName())
                                    .typeArguments(selfTypeArgs)
                    ).boundKind(BoundKind.EXTENDS))
            );

            typeArguments.addAll(
                    typeArgs.stream()
                            .map(name -> new TypeVarExpr().name(name))
                            .toList()
            );

            model.typeArguments(typeArguments);
        }
    }

    private void addProperties(final OpenAPI openAPI,
                               final HttpMethod httpMethod,
                               final Schema<?> schema,
                               final Model model, final HashSet<String> processedProperties) {
        addInheredProperties(openAPI, httpMethod, schema, model, processedProperties);

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

    private void addInheredProperties(final OpenAPI openAPI,
                                      final HttpMethod httpMethod,
                                      final Schema<?> schema,
                                      final Model model, final HashSet<String> processedProperties) {
        if (schema.getAllOf() != null) {
            Schema<?> ignoreSchema;

            if (schema.getAllOf().size() == 1) {
                ignoreSchema = SchemaResolver.resolve(openAPI, schema.getAllOf().getFirst()).schema();
                collectPropertyNames(ignoreSchema, processedProperties);

                final var parentType = resolveType(
                        schema.getAllOf().getFirst(),
                        httpMethod == HttpMethod.PATCH,
                        openAPI,
                        false);

                if (parentType instanceof ClassOrInterfaceTypeExpr referenceType) {
                    model.superType(referenceType);
                    final List<Map<String, String>> typeArgs = ExtensionsHelper.getExtension(
                            schema.getExtensions(),
                            Extensions.SUPER_TYPE_ARGS,
                            List.class
                    );

                    if (!typeArgs.isEmpty()) {
                        referenceType.typeArgument(
                                new ClassOrInterfaceTypeExpr()
                                        .packageName(model.getPackageName())
                                        .simpleName(model.getSimpleName())
                        );

                        for (final Map<String, String> typeArg : typeArgs) {
                            final var ref = typeArg.get("$ref");

                            if (ref != null) {
                                final var resolved = SchemaResolver.resolve(
                                        openAPI,
                                        ref
                                );

                                if (resolved.schema() != null) {
                                    final var type = (ClassOrInterfaceTypeExpr) resolveType(
                                            resolved.schema(),
                                            httpMethod == HttpMethod.PATCH,
                                            openAPI,
                                            true
                                    );
                                    type.packageName(modelPackageName);
                                    type.simpleName(resolved.name());
                                    referenceType.typeArgument(type);
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

        final var resolvedSchemaResult = SchemaResolver.resolve(openAPI, propertySchema);
        final var resolvedPropertySchema = resolvedSchemaResult.schema();

        if (resolvedPropertySchema != null) {
            final var isPatch = httpMethod == HttpMethod.PATCH
                    ? true
                    : null;

            var type = resolveType(resolvedPropertySchema, isPatch, openAPI, false);

            if (resolvedPropertySchema instanceof StringSchema stringSchema
                    && stringSchema.getEnum() != null) {
                final var enumName = StringUtils.firstUpper(propertyName) + "Enum";
                type = new ClassOrInterfaceTypeExpr()
                        .simpleName(enumName);
            }

            if (resolvedPropertySchema instanceof MapSchema mapSchema) {
                final var keyType = typeUtils.createStringType(null);
                final TypeExpr valueType;

                if (mapSchema.getAdditionalProperties() instanceof Schema<?> additionalSchema) {
                    valueType = resolveType(additionalSchema, false, openAPI, false);
                } else {
                    valueType = new ClassOrInterfaceTypeExpr().packageName("java.lang").simpleName("Object");
                }

                ((ClassOrInterfaceTypeExpr) type).typeArguments(keyType, valueType);
            }

            if ((resolvedPropertySchema instanceof ObjectSchema || resolvedPropertySchema instanceof ComposedSchema)
                    && type instanceof ClassOrInterfaceTypeExpr referenceType) {
                referenceType.packageName(modelPackageName);
                referenceType.simpleName(resolvedSchemaResult.name());
            }

            if (httpMethod == HttpMethod.PATCH) {
                type = new ClassOrInterfaceTypeExpr()
                        .packageName("org.openapitools.jackson.nullable")
                        .simpleName("JsonNullable")
                        .typeArgument(type);
            }

            final var property = new ModelProperty()
                    .simpleName(propertyName)
                    .type(type);
            model.enclosedElement(property);
            processedProperties.add(propertyName);
        }
    }

    private TypeExpr resolveType(final Schema<?> schema,
                                 final Boolean isPatch,
                                 final OpenAPI openAPI,
                                 final boolean isRequired) {
        return switch (schema) {
            case IntegerSchema integerSchema -> resolveIntegerType(integerSchema, isPatch);
            case NumberSchema numberSchema -> typeUtils.createNumberType(numberSchema, isRequired);
            case StringSchema stringSchema -> typeUtils.createStringType(stringSchema);
            case EmailSchema ignored -> typeUtils.createStringType(null);
            case PasswordSchema ignored -> typeUtils.createStringType(null);
            case DateSchema ignored -> typeUtils.createDateType();
            case DateTimeSchema ignored -> typeUtils.createDateTimeType();
            case MapSchema ignored -> typeUtils.createMapType(openAPI, null, modelPackageName, null, isRequired);
            case ArraySchema arraySchema -> typeUtils.createArrayType(openAPI, arraySchema, modelPackageName, null);
            case UUIDSchema ignored -> typeUtils.createUuidType();
            case BooleanSchema booleanSchema -> typeUtils.createBooleanType(booleanSchema, isRequired);
            case ObjectSchema objectSchema -> {
                final var typeArg = ExtensionsHelper.getExtension(
                        objectSchema.getExtensions(),
                        Extensions.TYPE_ARG,
                        String.class
                );

                if (typeArg == null) {
                    yield new ClassOrInterfaceTypeExpr().packageName("java.lang").simpleName("Object");
                } else {
                    yield new ClassOrInterfaceTypeExpr().simpleName(typeArg);
                }
            }
            default -> {
                if (schema.get$ref() != null) {
                    final var result = SchemaResolver.resolve(openAPI, schema);
                    if (result.schema() instanceof ObjectSchema) {
                        yield new ClassOrInterfaceTypeExpr().packageName(this.modelPackageName).simpleName(result.name());
                    }
                }

                throw new UnsupportedOperationException("resolveType " + schema);
            }
        };
    }

    private TypeExpr resolveIntegerType(final IntegerSchema schema,
                                        final Boolean isPatch) {
        final var format = schema.getFormat();

        if (Boolean.TRUE.equals(schema.getNullable()) || Boolean.TRUE.equals(isPatch)) {
            return "int64".equals(format)
                    ? new ClassOrInterfaceTypeExpr().packageName("java.lang").simpleName("Long")
                    : new ClassOrInterfaceTypeExpr().packageName("java.lang").simpleName("Integer");
        } else {
            return "int64".equals(format)
                    ? new PrimitiveTypeExpr(TypeKind.LONG)
                    : new PrimitiveTypeExpr(TypeKind.INT);
        }
    }

}
