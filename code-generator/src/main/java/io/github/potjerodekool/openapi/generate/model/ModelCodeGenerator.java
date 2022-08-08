package io.github.potjerodekool.openapi.generate.model;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import io.github.potjerodekool.openapi.Filer;
import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.RequestCycleLocation;
import io.github.potjerodekool.openapi.generate.GenerateUtils;
import io.github.potjerodekool.openapi.generate.Types;
import io.github.potjerodekool.openapi.tree.*;
import io.github.potjerodekool.openapi.type.OpenApiArrayType;
import io.github.potjerodekool.openapi.type.OpenApiObjectType;
import io.github.potjerodekool.openapi.type.OpenApiType;
import io.github.potjerodekool.openapi.util.GenerateException;
import io.github.potjerodekool.openapi.util.Utils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.IOException;
import java.util.HashSet;

import static io.github.potjerodekool.openapi.generate.GenerateHelper.*;

public class ModelCodeGenerator {

    private static final String JSON_NULLABLE_CLASS_NAME = "org.openapitools.jackson.nullable.JsonNullable";

    private final Types types;

    private final GenerateUtils generateUtils;
    private final Filer filer;

    private final ModelAdapter modelAdapter;

    public ModelCodeGenerator(final OpenApiGeneratorConfig config,
                              final Types types,
                              final GenerateUtils generateUtils,
                              final Filer filer) {
        this.types = types;
        this.generateUtils = generateUtils;
        this.filer = filer;
        this.modelAdapter = new CombinedModelAdapter(config, types, generateUtils);
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
        if (type instanceof OpenApiObjectType) {
            final var ot = (OpenApiObjectType) type;

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

            final var cu = new CompilationUnit();

            if (!pck.isUnnamed()) {
                cu.setPackageDeclaration(pck.getName());
            }

            final var clazz = cu.addClass(name);

            ot.properties().entrySet().stream()
                    .filter(entry -> propertyFilter(entry.getValue(), requestCycleLocation))
                    .forEach(entry -> addField(entry.getKey(), entry.getValue(), clazz, httpMethod, requestCycleLocation));

            generateConstructors(ot, clazz, httpMethod, requestCycleLocation);

            ot.properties().entrySet().stream()
                    .filter(it -> this.propertyFilter(it.getValue(), requestCycleLocation))
                            .forEach(entry -> {
                                final var propertyName = entry.getKey();
                                final var property = entry.getValue();

                                addGetter(propertyName, property, clazz, httpMethod, requestCycleLocation);

                                if (!hasFinalField(clazz, propertyName)) {
                                    addSetter(propertyName, property, clazz, false, httpMethod, requestCycleLocation);
                                    addSetter(propertyName, property, clazz, true, httpMethod, requestCycleLocation);
                                }
                            });

            try {
                filer.write(cu);
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

    private boolean hasFinalField(final ClassOrInterfaceDeclaration clazz,
                                  final String propertyName) {
        final var fieldOptional = clazz.getFieldByName(propertyName);

        if (fieldOptional.isEmpty()) {
            return false;
        }

        return isFieldFinal(fieldOptional.get());
    }

    private boolean isFieldFinal(final FieldDeclaration field) {
        return field.getModifiers().stream()
                .anyMatch(modifier -> modifier.getKeyword() == Modifier.Keyword.FINAL);
    }

    private void generateConstructors(final OpenApiObjectType ot,
                                      final ClassOrInterfaceDeclaration clazz,
                                      final HttpMethod httpMethod, RequestCycleLocation requestCycleLocation) {
        final var constructor = clazz.addConstructor(Modifier.Keyword.PUBLIC);

        final var initPropertyNames = new HashSet<String>();

        ot.properties().entrySet().stream()
                .filter(it -> this.propertyFilter(it.getValue(), requestCycleLocation))
                .forEach(entry -> {
                    final var propertyName = entry.getKey();
                    final var property = entry.getValue();

                    final var isFieldFinal = hasFinalField(clazz, propertyName);

                    if (property.required() || isFieldFinal) {
                        final var paramType = types.createType(property.type());
                        final var parameter = new Parameter(paramType, propertyName);
                        parameter.addModifier(Modifier.Keyword.FINAL);
                        constructor.addParameter(parameter);
                        initPropertyNames.add(propertyName);
                    }
                });

        final var body = new BlockStmt();

        ot.properties().entrySet().stream()
                .filter(entry -> initPropertyNames.contains(entry.getKey()))
                .forEach(entry -> {
                    Expression value = new NameExpr(entry.getKey());

                    if (httpMethod == HttpMethod.PATCH) {
                        value = new MethodCallExpr(
                                new NameExpr(JSON_NULLABLE_CLASS_NAME),
                                "of",
                                NodeList.nodeList(value)
                        );
                    }

                    body.addStatement(new AssignExpr(
                            new FieldAccessExpr(new ThisExpr(), entry.getKey()),
                            value,
                            AssignExpr.Operator.ASSIGN
                    ));
                });

        constructor.setBody(body);
    }

    private void addField(final String propertyName,
                          final OpenApiProperty property,
                          final ClassOrInterfaceDeclaration clazz,
                          final HttpMethod httpMethod,
                          final RequestCycleLocation requestCycleLocation) {
        final var propertyType = property.type();
        var fieldType = types.createType(propertyType);

        final var isPatch = httpMethod == HttpMethod.PATCH;

        if (isPatch) {
            if (fieldType.isPrimitiveType()) {
                fieldType = types.getBoxedType(fieldType);
            }

            fieldType = types.createType(JSON_NULLABLE_CLASS_NAME)
                    .setTypeArguments(fieldType);
        }

        final var field = clazz.addField(fieldType, propertyName, Modifier.Keyword.PRIVATE);

        if (property.required()) {
            field.addModifier(Modifier.Keyword.FINAL);
        } else {
            if (isPatch) {
                final var defaultValue = new MethodCallExpr(
                        new NameExpr(JSON_NULLABLE_CLASS_NAME),
                        "undefined");
                final var variable = field.getVariable(0);
                variable.setInitializer(defaultValue);
            } else {
                final var defaultValue = generateUtils.getDefaultValue(fieldType);

                if (defaultValue != null) {
                    final var variable = field.getVariable(0);
                    variable.setInitializer(defaultValue);
                }
            }
        }

        if (propertyType instanceof OpenApiArrayType) {
            var elementType = getFirstTypeArg(fieldType);

            if (isPatch) {
                elementType = getFirstTypeArg(elementType);
            }

            field.addAnnotation(createArraySchemaAnnotation(elementType));
        } else {
            final var type = types.createType(propertyType);
            field.addAnnotation(createSchemaAnnotation(type, property.required()));
        }

        this.modelAdapter.adaptField(httpMethod, requestCycleLocation, property, field);
    }

    private void addGetter(final String propertyName,
                           final OpenApiProperty property,
                           final ClassOrInterfaceDeclaration clazz,
                           final HttpMethod httpMethod,
                           final RequestCycleLocation requestCycleLocation) {
        final var methodName = "get" + Utils.firstUpper(propertyName);
        final var method = clazz.addMethod(
                methodName,
                Modifier.Keyword.PUBLIC
        );

        final var propertyType = property.type();
        var returnType = types.createType(propertyType);

        if (httpMethod == HttpMethod.PATCH) {
            if (returnType.isPrimitiveType()) {
                returnType = types.getBoxedType(returnType);
            }

            returnType = types.createType(JSON_NULLABLE_CLASS_NAME)
                    .setTypeArguments(returnType);
        }

        method.setType(returnType);

        final var body = new BlockStmt();
        body.addStatement(new ReturnStmt(new FieldAccessExpr(new ThisExpr(), propertyName)));
        method.setBody(body);

        modelAdapter.adaptGetter(httpMethod, requestCycleLocation, property, method);
    }

    private void addSetter(final String propertyName,
                           final OpenApiProperty property,
                           final ClassOrInterfaceDeclaration clazz,
                           final boolean isBuilder,
                           final HttpMethod httpMethod,
                           final RequestCycleLocation requestCycleLocation) {
        final var methodName = isBuilder ? propertyName : "set" + Utils.firstUpper(propertyName);
        final var method = clazz.addMethod(
                methodName,
                Modifier.Keyword.PUBLIC
        );

        final var propertyType = property.type();
        final var parameterType = types.createType(propertyType);
        final var parameter = new Parameter(parameterType, propertyName);
        parameter.addModifier(Modifier.Keyword.FINAL);

        method.addParameter(parameter);

        final var body = new BlockStmt();

        Expression varExpression = new NameExpr(propertyName);

        if (httpMethod == HttpMethod.PATCH) {
            varExpression = new MethodCallExpr(
                    new NameExpr(JSON_NULLABLE_CLASS_NAME),
                    "of",
                    NodeList.nodeList(varExpression)
            );
        }

        body.addStatement(new AssignExpr(
                new FieldAccessExpr(new ThisExpr(), propertyName),
                varExpression,
                AssignExpr.Operator.ASSIGN
        ));

        if (isBuilder) {
            body.addStatement(new ReturnStmt(new ThisExpr()));
            method.setType(types.createType(clazz.getName().toString()));
        }

        method.setBody(body);

        modelAdapter.adaptSetter(httpMethod, requestCycleLocation, property, method);
    }
}
