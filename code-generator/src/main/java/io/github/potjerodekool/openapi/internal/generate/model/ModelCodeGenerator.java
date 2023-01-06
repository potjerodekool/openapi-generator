package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.Language;
import io.github.potjerodekool.openapi.RequestCycleLocation;
import io.github.potjerodekool.openapi.internal.Filer;
import io.github.potjerodekool.openapi.internal.ast.Attribute;
import io.github.potjerodekool.openapi.internal.ast.CompilationUnit;
import io.github.potjerodekool.openapi.internal.ast.Modifier;
import io.github.potjerodekool.openapi.internal.ast.Operator;
import io.github.potjerodekool.openapi.internal.ast.element.*;
import io.github.potjerodekool.openapi.internal.ast.expression.*;
import io.github.potjerodekool.openapi.internal.ast.statement.BlockStatement;
import io.github.potjerodekool.openapi.internal.ast.statement.ReturnStatement;
import io.github.potjerodekool.openapi.internal.ast.type.*;
import io.github.potjerodekool.openapi.internal.ast.type.java.WildcardType;
import io.github.potjerodekool.openapi.internal.ast.util.TypeUtils;
import io.github.potjerodekool.openapi.internal.di.ApplicationContext;
import io.github.potjerodekool.openapi.internal.util.GenerateException;
import io.github.potjerodekool.openapi.internal.util.Utils;
import io.github.potjerodekool.openapi.tree.*;
import io.github.potjerodekool.openapi.type.OpenApiArrayType;
import io.github.potjerodekool.openapi.type.OpenApiObjectType;
import io.github.potjerodekool.openapi.type.OpenApiType;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.lang.model.type.TypeKind;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class ModelCodeGenerator {

    private static final String JSON_NULLABLE_CLASS_NAME = "org.openapitools.jackson.nullable.JsonNullable";

    private final TypeUtils typeUtils;

    private final Filer filer;
    private final Language language;

    private final CombinedModelAdapter modelAdapter;

    public ModelCodeGenerator(final Filer filer,
                              final TypeUtils typeUtils,
                              final ApplicationContext applicationContext,
                              final Language language) {
        this.filer = filer;
        this.typeUtils = typeUtils;
        this.language = language;
        this.modelAdapter = new CombinedModelAdapter(applicationContext);
    }

    public void generate(final OpenApi api) {
        api.paths().forEach(this::processPath);
    }

    private void processPath(final OpenApiPath path) {
        processOperation(HttpMethod.POST, path.post());
        processOperation(HttpMethod.GET, path.get());
        processOperation(HttpMethod.PUT, path.put());
        processOperation(HttpMethod.PATCH, path.patch());
    }

    private void processOperation(final HttpMethod httpMethod,
                                  final @Nullable OpenApiOperation operation) {
        if (operation == null) {
            return;
        }

        processRequestBody(httpMethod, operation.requestBody());
        operation.responses().values()
                        .forEach(response -> processResponse(httpMethod, response));
    }

    private void processRequestBody(final HttpMethod httpMethod,
                                    final @Nullable OpenApiRequestBody requestBody) {
        if (requestBody == null) {
            return;
        }

        requestBody.contentMediaType().values()
                .forEach(content -> processMediaType(httpMethod, content.schema(), RequestCycleLocation.REQUEST));
    }


    private void processResponse(final HttpMethod httpMethod,
                                 final @Nullable OpenApiResponse openApiResponse) {
        if (openApiResponse == null) {
            return;
        }

        openApiResponse.contentMediaType().values()
                .forEach(content -> processMediaType(httpMethod, content.schema(), RequestCycleLocation.RESPONSE));
    }

    private void processMediaType(final HttpMethod httpMethod,
                                  final OpenApiType type,
                                  final RequestCycleLocation requestCycleLocation) {
        if (type instanceof OpenApiObjectType ot) {
            final var pck = ot.pck();
            final var typeName = ot.name();

            ot.properties().values().stream()
                    .map(OpenApiProperty::type)
                    .forEach(propertyType -> processMediaType(httpMethod, propertyType, requestCycleLocation));

            final var additionalProperty = ot.additionalProperties();

            if (additionalProperty != null) {
                processMediaType(httpMethod, additionalProperty.type(), requestCycleLocation);
            }

            if ("object".equals(typeName) && pck.isUnnamed()) {
                //Don't generate object
                return;
            }

            final var name = Utils.firstUpper(typeName);

            final var cu = new CompilationUnit(Language.JAVA);

            cu.setPackageElement(PackageElement.create(pck.getName()));

            final var typeElement = cu.addClass(name).addModifier(Modifier.PUBLIC);

            ot.properties().entrySet().stream()
                    .filter(entry -> propertyFilter(entry.getValue(), requestCycleLocation))
                    .forEach(entry -> addField(entry.getKey(), entry.getValue(), typeElement, httpMethod, requestCycleLocation));

            var constructor = generateConstructor(ot, typeElement, httpMethod, requestCycleLocation, false);

            if (shouldGenerateAllArgConstructor(ot, requestCycleLocation, typeElement)) {
                constructor = generateConstructor(ot, typeElement, httpMethod, requestCycleLocation, true);
            }

            constructor.addAnnotation("com.fasterxml.jackson.annotation.JsonCreator");

            ot.properties().entrySet().stream()
                    .filter(it -> this.propertyFilter(it.getValue(), requestCycleLocation))
                            .forEach(entry -> {
                                final var propertyName = entry.getKey();
                                final var property = entry.getValue();

                                addGetter(propertyName, property, typeElement, httpMethod, requestCycleLocation);

                                if (!hasFinalField(typeElement, propertyName)) {
                                    addSetter(propertyName, property, typeElement, false, httpMethod, requestCycleLocation);
                                    addSetter(propertyName, property, typeElement, true, httpMethod, requestCycleLocation);
                                }
                            });

            try {
                filer.writeSource(cu, language);
            } catch (final IOException e) {
                throw new GenerateException(e);
            }
        } else if (type instanceof OpenApiArrayType at) {
            processMediaType(httpMethod, at.items(), requestCycleLocation);
        }
    }

    private boolean propertyFilter(final OpenApiProperty property,
                                   final RequestCycleLocation requestCycleLocation) {
        return Boolean.TRUE.equals(property.readOnly())
                ? requestCycleLocation == RequestCycleLocation.RESPONSE
                : !Boolean.TRUE.equals(property.writeOnly()) || requestCycleLocation == RequestCycleLocation.REQUEST;
    }

    private boolean constructorParameterFilter(final OpenApiProperty property,
                                               final RequestCycleLocation requestCycleLocation,
                                               final boolean allArg) {
        if (requestCycleLocation == RequestCycleLocation.REQUEST) {
            if (Boolean.TRUE.equals(property.readOnly())) {
                return false;
            }
        }

        return property.required() || allArg;
    }

    private boolean hasFinalField(final TypeElement typeElement,
                                  final String propertyName) {
        final var fieldOptional =
                ElementFilter.fields(typeElement)
                        .filter(field -> field.getSimpleName().equals(propertyName))
                        .findFirst();

        if (fieldOptional.isEmpty()) {
            return false;
        }

        return fieldOptional.get().isFinal();
    }

    private boolean shouldGenerateAllArgConstructor(final OpenApiObjectType ot,
                                                    final RequestCycleLocation requestCycleLocation,
                                                    final TypeElement typeElement) {
        final var allArgConstructorParameterCount = ot.properties().entrySet().stream()
                .filter(it -> this.propertyFilter(it.getValue(), requestCycleLocation))
                .count();

        final var constructors = ElementFilter.constructors(typeElement).toList();

        if (constructors.isEmpty()) {
            return true;
        }
        
        final var existingConstructor = constructors.get(0);
        return existingConstructor.getParameters().size() < allArgConstructorParameterCount;
    }

    private MethodElement generateConstructor(final OpenApiObjectType ot,
                                              final TypeElement typeElement,
                                              final HttpMethod httpMethod, RequestCycleLocation requestCycleLocation,
                                              final boolean allArg) {
        final var initPropertyNames = new HashSet<String>();

        final var parameters = new ArrayList<VariableElement>();

        ot.properties().entrySet().stream()
                .filter(it -> this.constructorParameterFilter(it.getValue(), requestCycleLocation, allArg))
                .forEach(entry -> {
                    final var propertyName = entry.getKey();
                    final var property = entry.getValue();
                    var paramType = typeUtils.createType(property.type());

                    if (httpMethod == HttpMethod.PATCH && requestCycleLocation == RequestCycleLocation.REQUEST) {
                        paramType = typeUtils.createDeclaredType(JSON_NULLABLE_CLASS_NAME)
                                .withTypeArgument(paramType.asNullableType());
                    }

                    parameters.add(VariableElement.createParameter(propertyName, paramType)
                            .addModifier(Modifier.FINAL)
                    );
                    initPropertyNames.add(propertyName);
                });

        final var body = new BlockStatement();

        ot.properties().entrySet().stream()
                .filter(entry -> initPropertyNames.contains(entry.getKey()))
                .forEach(entry -> {
                    final var value = new NameExpression(entry.getKey());

                    body.add(new BinaryExpression(
                                new FieldAccessExpression(
                                        new NameExpression("this"),
                                        entry.getKey()
                                ),
                                value,
                                Operator.ASSIGN
                        )
                      );
                });


        final var constructor = typeElement.addConstructor(Modifier.PUBLIC);
        parameters.forEach(constructor::addParameter);

        constructor.setBody(body);
        modelAdapter.adaptConstructor(httpMethod, requestCycleLocation, constructor);
        return constructor;
    }

    private void addField(final String propertyName,
                          final OpenApiProperty property,
                          final TypeElement clazz,
                          final HttpMethod httpMethod,
                          final RequestCycleLocation requestCycleLocation) {
        final var propertyType = property.type();
        var fieldType = typeUtils.createType(propertyType);

        final var isPatchRequest = httpMethod == HttpMethod.PATCH && requestCycleLocation == RequestCycleLocation.REQUEST;

        if (isPatchRequest) {
            if (fieldType.isPrimitiveType()) {
                fieldType = typeUtils.getBoxedType(fieldType);
            }

            fieldType = typeUtils.createDeclaredType(JSON_NULLABLE_CLASS_NAME)
                    .withTypeArgument(fieldType);
        }

        final var field = clazz.addField(fieldType, propertyName, Modifier.PRIVATE);

        if (property.required()) {
            field.addModifier(Modifier.FINAL);
        } else {
            if (isPatchRequest) {
                final var defaultValue = new MethodCallExpression(
                        new NameExpression(JSON_NULLABLE_CLASS_NAME),
                        "undefined");
                field.setInitExpression(defaultValue);
            } else {
                if (fieldType.isDeclaredType() && !fieldType.isNullable()) {
                    field.setInitExpression(null);
                } else {
                    final var defaultValue = getDefaultValue(fieldType);

                    if (defaultValue != null) {
                        field.setInitExpression(defaultValue);
                    }
                }
            }
        }

        if (propertyType instanceof OpenApiArrayType) {
            Type<?> elementType;

            if (fieldType.isArrayType()) {
                final var arrayType = (ArrayType) fieldType;
                elementType = arrayType.getComponentType();
            } else {
                elementType = fieldType.getFirstTypeArg().get();
            }

            if (isPatchRequest) {
                if (elementType.isArrayType()) {
                    final var arrayType = (ArrayType) elementType;
                    elementType = arrayType.getComponentType();
                } else {
                    elementType = elementType.getFirstTypeArg().get();
                }
            }

            field.addAnnotation(createArraySchemaAnnotation(elementType));
        } else {
            final var type = typeUtils.createType(propertyType);
            field.addAnnotation(createSchemaAnnotation(type, property.required(), property.description()));
        }

        this.modelAdapter.adaptField(httpMethod, requestCycleLocation, property, field);
    }

    private void addGetter(final String propertyName,
                           final OpenApiProperty property,
                           final TypeElement typeElement,
                           final HttpMethod httpMethod,
                           final RequestCycleLocation requestCycleLocation) {
        final var methodName = "get" + Utils.firstUpper(propertyName);
        final var method = typeElement.addMethod(
                methodName,
                Modifier.PUBLIC
        );

        final var propertyType = property.type();
        var returnType = typeUtils.createType(propertyType);

        final var isPatchRequest = httpMethod == HttpMethod.PATCH && requestCycleLocation == RequestCycleLocation.REQUEST;

        if (isPatchRequest) {
            if (returnType.isPrimitiveType()) {
                returnType = typeUtils.getBoxedType(returnType);
            }

            returnType = typeUtils.createDeclaredType(JSON_NULLABLE_CLASS_NAME)
                    .withTypeArgument(returnType);
        }

        method.setReturnType(returnType);

        final var body = new BlockStatement(
                new ReturnStatement(new FieldAccessExpression(new NameExpression("this"), propertyName))
        );
        method.setBody(body);

        modelAdapter.adaptGetter(httpMethod, requestCycleLocation, property, method);
    }

    private void addSetter(final String propertyName,
                           final OpenApiProperty property,
                           final TypeElement clazz,
                           final boolean isBuilder,
                           final HttpMethod httpMethod,
                           final RequestCycleLocation requestCycleLocation) {
        final var methodName = isBuilder ? propertyName : "set" + Utils.firstUpper(propertyName);
        final var method = clazz.addMethod(
                methodName,
                Modifier.PUBLIC
        );

        final var propertyType = property.type();
        var parameterType = typeUtils.createType(propertyType);


        final var isPatchRequest = httpMethod == HttpMethod.PATCH && requestCycleLocation == RequestCycleLocation.REQUEST;

        if (isPatchRequest) {
            if (parameterType.isPrimitiveType()) {
                parameterType = typeUtils.getBoxedType(parameterType);
            }
            parameterType = typeUtils.createDeclaredType(JSON_NULLABLE_CLASS_NAME)
                    .withTypeArgument(parameterType);
        }

        final var parameter = VariableElement.createParameter(propertyName, parameterType);
        parameter.addModifier(Modifier.FINAL);

        method.addParameter(parameter);

        final var body = new BlockStatement();

        final var varExpression = new NameExpression(propertyName);

        body.add(new BinaryExpression(
                new FieldAccessExpression(new NameExpression("this"), propertyName),
                varExpression,
                Operator.ASSIGN
        ));

        if (isBuilder) {
            body.add(new ReturnStatement(new NameExpression("this")));
            method.setReturnType(typeUtils.createDeclaredType(clazz.getQualifiedName()));
        }

        method.setBody(body);

        modelAdapter.adaptSetter(httpMethod, requestCycleLocation, property, method);
    }

    public Attribute.Compound createSchemaAnnotation(final Type<?> type,
                                                     final Boolean required,
                                                     final @Nullable String description) {
        final var members = new HashMap<ExecutableElement, AnnotationValue>();

        final Type<?> implementationType;

        if (type instanceof WildcardType wt) {
            if (wt.getExtendsBound().isPresent()) {
                implementationType = wt.getExtendsBound().get();
            } else if (wt.getSuperBound().isPresent()) {
                implementationType = wt.getSuperBound().get();
            } else {
                //Will result in compilation error in generated code.
                implementationType = wt;
            }
        } else if (typeUtils.isMapType(type)) {
            implementationType = typeUtils.createMapType();
        } else {
            implementationType = type;
        }

        members.put(
                MethodElement.createMethod("implementation"),
                Attribute.clazz(implementationType)
        );

        if (Utils.isTrue(required)) {
            members.put(
                    MethodElement.createMethod("required"),
                    Attribute.constant(true));
        }

        if (description != null) {
            members.put(
                    MethodElement.createMethod(("description")),
                    Attribute.constant(description)
            );
        }

        return Attribute.compound(
                "io.swagger.v3.oas.annotations.media.Schema",
                members
        );
    }

    public AnnotationMirror createArraySchemaAnnotation(final Type<?> elementType) {
        return createAnnotation("io.swagger.v3.oas.annotations.media.ArraySchema", "schema",
                createSchemaAnnotation(elementType, false, null)
        );
    }

    public AnnotationMirror createAnnotation(final String name,
                                             final String memberName,
                                             final Attribute value) {
        return Attribute.compound(
                name,
                Map.of(
                        MethodElement.createMethod(memberName),
                        value
                )
        );
    }

    public @Nullable Expression getDefaultValue(final Type<?> type) {
        if (type.isPrimitiveType()) {
            final var primitiveType = ((PrimitiveType) type);

            return switch (primitiveType.getKind()) {
                case BOOLEAN -> LiteralExpression.createBooleanLiteralExpression();
                case CHAR -> LiteralExpression.createCharLiteralExpression();
                case BYTE, SHORT, INT -> LiteralExpression.createIntLiteralExpression();
                case LONG -> LiteralExpression.createLongLiteralExpression();
                case FLOAT -> LiteralExpression.createFloatLiteralExpression();
                case DOUBLE -> LiteralExpression.createDoubleLiteralExpression();
                default -> throw new GenerateException(String.format("%s is not a primitive", primitiveType.getKind()));
            };
        } else if (typeUtils.isListType(type)) {
            return new MethodCallExpression(
                    new NameExpression(typeUtils.getListTypeName()),
                    "of"
            );
        } else if (type.getKind() == TypeKind.DECLARED) {
            return LiteralExpression.createNullLiteralExpression();
        } else if (type.isArrayType()) {
            return new ArrayInitializerExpression();
        } else {
            return null;
        }
    }
}
