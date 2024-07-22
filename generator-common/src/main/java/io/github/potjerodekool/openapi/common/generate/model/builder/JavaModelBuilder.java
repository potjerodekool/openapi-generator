package io.github.potjerodekool.openapi.common.generate.model.builder;

import io.github.potjerodekool.codegen.model.tree.type.BoundKind;
import io.github.potjerodekool.codegen.model.type.TypeKind;
import io.github.potjerodekool.codegen.model.util.StringUtils;
import io.github.potjerodekool.codegen.template.model.type.*;
import io.github.potjerodekool.openapi.common.SchemaType;
import io.github.potjerodekool.openapi.common.SchemaTypeResolver;
import io.github.potjerodekool.openapi.common.generate.*;
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
                       final Schema<?> schema,
                       final ContentType contentType) {
        final var model = new Model();
        model.kind(resolveKind(schema));
        model.simpleName(name);
        model.packageName(packageName);
        final var processedProperties = new HashSet<String>();
        addProperties(
                openAPI,
                httpMethod,
                schema,
                contentType,
                model,
                processedProperties
        );
        postProcessProperties(schema, model);
        processModelExtensions(schema, model);
        return model;
    }

    private Element.Kind resolveKind(final Schema<?> schema) {
        final var schemaKind = SchemaTypeResolver.resolveSchemaType(schema);
        return schemaKind == SchemaType.ENUM
                ? Element.Kind.ENUM
                : Element.Kind.CLASS;
    }

    private void postProcessProperties(final Schema<?> schema,
                                       final Model model) {
        if (schema.getProperties() == null) {
            return;
        }

        schema.getProperties().entrySet().stream()
                .filter(entry -> SchemaTypeResolver.resolveSchemaType(entry.getValue()) == SchemaType.ENUM)
                .forEach(entry -> {
                    final var enumName = StringUtils.firstUpper(entry.getKey());
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

    private void processModelExtensions(final Schema<?> resolvedSchema,
                                        final Model model) {
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
                               final ContentType contentType,
                               final Model model,
                               final HashSet<String> processedProperties) {
        if (model.getKind() == Element.Kind.ENUM) {
            if (schema.getEnum() != null) {
                final var enumValues = (List<String>) schema.getEnum();

                enumValues.forEach(enumValue -> {
                    final var property = new ModelProperty();
                    property.kind(Element.Kind.ENUM_CONSTANT);
                    property.simpleName(enumValue);
                    model.enclosedElement(property);
                    property.enclosingElement(model);
                });
            }
        } else {
            addInheredProperties(
                    openAPI,
                    httpMethod,
                    schema,
                    contentType,
                    model,
                    processedProperties
            );

            if (schema.getProperties() != null) {
                schema.getProperties()
                        .forEach((propertyName, propertySchema) ->
                                addProperty(
                                        openAPI,
                                        httpMethod,
                                        propertyName,
                                        propertySchema,
                                        contentType,
                                        model,
                                        processedProperties
                                ));
            }
        }
    }

    private void addInheredProperties(final OpenAPI openAPI,
                                      final HttpMethod httpMethod,
                                      final Schema<?> schema,
                                      final ContentType contentType,
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
                    addProperties(
                            openAPI,
                            httpMethod,
                            resolved.schema(),
                            contentType,
                            model,
                            processedProperties
                    );
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
                             final Schema<?> propertySchema,
                             final ContentType contentType,
                             final Model model,
                             final HashSet<String> processedProperties) {
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
            final var schemaType = SchemaTypeResolver.resolveSchemaType(resolvedPropertySchema);

            if (schemaType == SchemaType.ENUM) {
                if (propertySchema.get$ref() != null) {
                    type = new ClassOrInterfaceTypeExpr()
                            .packageName(modelPackageName)
                            .simpleName(resolvedSchemaResult.name());
                } else {
                    final var enumName = StringUtils.firstUpper(propertyName);
                    type = new ClassOrInterfaceTypeExpr()
                            .packageName(modelPackageName + "." + model.getSimpleName())
                            .simpleName(enumName);
                }
            }

            if (resolvedSchemaResult.name() != null
                    && (resolvedPropertySchema instanceof ObjectSchema || resolvedPropertySchema instanceof ComposedSchema)
                    && type instanceof ClassOrInterfaceTypeExpr referenceType) {
                referenceType.packageName(modelPackageName);
                referenceType.simpleName(resolvedSchemaResult.name());
            }

            if (httpMethod == HttpMethod.PATCH
                    && !ContentType.JSON_PATCH_JSON.equals(contentType)) {
                type = new ClassOrInterfaceTypeExpr()
                        .packageName("org.openapitools.jackson.nullable")
                        .simpleName("JsonNullable")
                        .typeArgument(type);
            }

            final var property = new ModelProperty()
                    .simpleName(propertyName)
                    .type(type);
            model.enclosedElement(property);
            property.enclosingElement(model);

            processedProperties.add(propertyName);
        }
    }

    private TypeExpr resolveType(final Schema<?> schema,
                                 final Boolean isPatch,
                                 final OpenAPI openAPI,
                                 final boolean isRequired) {
        return switch (SchemaTypeResolver.resolveSchemaType(schema)) {
            case INT32, INT64 -> resolveIntegerType(schema, isPatch);
            case FLOAT, DOUBLE -> typeUtils.createNumberType(schema, isRequired);
            case STRING, EMAIL, PASSWORD -> typeUtils.createStringType(schema);
            case DATE -> typeUtils.createDateType();
            case DATE_TIME -> typeUtils.createDateTimeType();
            case MAP -> typeUtils.createMapType(openAPI, schema, modelPackageName, null, isRequired);
            case ARRAY -> typeUtils.createArrayType(openAPI, schema, modelPackageName, null);
            case UUID -> typeUtils.createUuidType();
            case BOOLEAN -> typeUtils.createBooleanType(schema, isRequired);
            case OBJECT -> {
                final var typeArg = ExtensionsHelper.getExtension(
                        schema.getExtensions(),
                        Extensions.TYPE_ARG,
                        String.class
                );

                if (typeArg == null) {
                    yield new ClassOrInterfaceTypeExpr().packageName("java.lang").simpleName("Object");
                } else {
                    yield new ClassOrInterfaceTypeExpr().simpleName(typeArg);
                }
            }
            case BINARY -> typeUtils.createMultipartTypeExpression(openAPI);
            case ENUM -> new ClassOrInterfaceTypeExpr()
                    .packageName(modelPackageName)
                    .simpleName("Enum");

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

    private TypeExpr resolveIntegerType(final Schema<?> schema,
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
