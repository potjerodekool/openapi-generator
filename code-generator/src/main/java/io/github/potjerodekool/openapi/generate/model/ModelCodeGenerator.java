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
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import io.github.potjerodekool.openapi.Filer;
import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.RequestCycleLocation;
import io.github.potjerodekool.openapi.generate.GenerateHelper;
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

import static com.github.javaparser.ast.NodeList.nodeList;
import static io.github.potjerodekool.openapi.generate.GenerateHelper.createAnnotation;
import static io.github.potjerodekool.openapi.generate.GenerateHelper.getDefaultValue;

public class ModelCodeGenerator {

    private final OpenApiGeneratorConfig config;
    private final Types types;
    private final Filer filer;

    private final String notNullAnnotationClassName;
    private final String validAnnotationClassName;

    public ModelCodeGenerator(final OpenApiGeneratorConfig config,
                              final Types types,
                              final Filer filer) {
        this.config = config;
        this.types = types;
        this.filer = filer;
        notNullAnnotationClassName = config.isUseJakartaValidation()
            ? "jakarta.validation.constraints.NotNull"
            : "javax.validation.constraints.NotNull";
        validAnnotationClassName = config.isUseJakartaValidation()
            ? "jakarta.validation.Valid"
            : "javax.validation.Valid";
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
                .forEach(mt -> processMediaType(httpMethod, mt, RequestCycleLocation.REQUEST));
    }


    private void processResponse(final HttpMethod httpMethod,
                                 final @Nullable OpenApiResponse openApiResponse) {
        if (openApiResponse == null) {
            return;
        }

        openApiResponse.contentMediaType().values()
                .forEach(mt -> processMediaType(httpMethod, mt, RequestCycleLocation.RESPONSE));
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

            final var cu = new CompilationUnit();

            if (!pck.isUnnamed()) {
                cu.setPackageDeclaration(pck.getName());
            }

            final var clazz = cu.addClass(name);

            ot.properties().forEach((propertyName, property) -> addField(propertyName, property, clazz, httpMethod, requestCycleLocation));

            generateConstructors(ot, clazz, httpMethod, requestCycleLocation);

            ot.properties()
                    .forEach((propertyName, property) -> {
                        if (property.readOnly() && requestCycleLocation == RequestCycleLocation.REQUEST) {
                            return;
                        }

                        addGetter(propertyName, property, clazz, httpMethod, requestCycleLocation);

                        if (!hasFinalField(clazz, propertyName)) {
                            addSetter(propertyName, property, clazz, false, httpMethod);
                            addSetter(propertyName, property, clazz, true, httpMethod);
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

        ot.properties().forEach((propertyName, property) -> {
            final var isFieldFinal = hasFinalField(clazz, propertyName);

            if (property.readOnly() && requestCycleLocation == RequestCycleLocation.REQUEST) {
                return;
            }

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
                                new NameExpr("org.openapitools.jackson.nullable.JsonNullable"),
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
                          final HttpMethod httpMethod, RequestCycleLocation requestCycleLocation) {
        if (property.readOnly() && requestCycleLocation == RequestCycleLocation.REQUEST) {
            return;
        }

        final var propertyType = property.type();
        var fieldType = types.createType(propertyType);

        final var isPatch = httpMethod == HttpMethod.PATCH;

        if (isPatch) {
            if (fieldType.isPrimitiveType()) {
                fieldType = types.getBoxedType(fieldType);
            }

            fieldType = types.createType("org.openapitools.jackson.nullable.JsonNullable")
                    .setTypeArguments(fieldType);
        }

        if (!isPatch
                && fieldType.isClassOrInterfaceType()
                && config.isAddCheckerAnnotations()) {
            fieldType.setAnnotations(
                    nodeList(new MarkerAnnotationExpr(new Name("org.checkerframework.checker.nullness.qual.Nullable")))
            );
        }

        final var field = clazz.addField(fieldType, propertyName, Modifier.Keyword.PRIVATE);

        if (property.required()) {
            field.addAnnotation(createAnnotation("io.swagger.annotations.ApiModelProperty", "required", true));
            field.addModifier(Modifier.Keyword.FINAL);
        } else {
            if (isPatch) {
                final var defaultValue = new MethodCallExpr(
                        new NameExpr("JsonNullable"),
                        "undefined");
                final var variable = field.getVariable(0);
                variable.setInitializer(defaultValue);
            } else {
                final var defaultValue = getDefaultValue(fieldType);

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

            field.addAnnotation(createAnnotation("io.swagger.v3.oas.annotations.media.ArraySchema", "schema",
                    createAnnotation(
                            "io.swagger.v3.oas.annotations.media.Schema", "implementation", new ClassExpr(elementType)))
            );
        } else {
            final var type = types.createType(propertyType);

            field.addAnnotation(
                    createAnnotation("io.swagger.v3.oas.annotations.media.Schema", "implementation", new ClassExpr(type))
            );
        }
    }

    private Type getFirstTypeArg(final Type type) {
        if (type instanceof ClassOrInterfaceType cType) {
            return cType.getTypeArguments()
                    .filter(it -> it.size() > 0)
                    .map(it -> it.get(0))
                    .orElseThrow(() -> new GenerateException("Expected a type argument"));
        } else {
            return type;
        }
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

        final var isPrimitiveType = returnType.isPrimitiveType();
        final var isListType = GenerateHelper.isListType(returnType);

        final var returnTypeAnnotations = new NodeList<AnnotationExpr>();

        if (httpMethod == HttpMethod.PATCH) {
            if (returnType.isPrimitiveType()) {
                returnType = types.getBoxedType(returnType);
            }

            returnType = types.createType("org.openapitools.jackson.nullable.JsonNullable")
                    .setTypeArguments(returnType);
        }

        if (!returnType.isPrimitiveType() &&
                !property.required() && config.isAddCheckerAnnotations() && httpMethod != HttpMethod.PATCH) {
            returnTypeAnnotations.add(new MarkerAnnotationExpr(new Name("org.checkerframework.checker.nullness.qual.Nullable")));
        }

        if (!isPrimitiveType && !isListType) {
            returnTypeAnnotations.add(new MarkerAnnotationExpr(notNullAnnotationClassName));
        }

        if (requestCycleLocation == RequestCycleLocation.REQUEST && property.type().format() != null) {
            method.addAnnotation(validAnnotationClassName);
        }

        if (!returnTypeAnnotations.isEmpty()) {
            returnType.setAnnotations(returnTypeAnnotations);
        }

        method.setType(returnType);

        final var body = new BlockStmt();

        body.addStatement(new ReturnStmt(new FieldAccessExpr(new ThisExpr(), propertyName)));

        method.setBody(body);
    }

    private void addSetter(final String propertyName,
                           final OpenApiProperty property,
                           final ClassOrInterfaceDeclaration clazz,
                           final boolean isBuilder,
                           final HttpMethod httpMethod) {
        final var methodName = isBuilder ? propertyName : "set" + Utils.firstUpper(propertyName);
        final var method = clazz.addMethod(
                methodName,
                Modifier.Keyword.PUBLIC
        );

        final var propertyType = property.type();
        final var isNullable = propertyType.nullable();
        final var parameterType = types.createType(propertyType);

        if (config.isAddCheckerAnnotations()) {
            if (Boolean.TRUE.equals(isNullable)) {
                parameterType.setAnnotations(
                        nodeList(
                                new MarkerAnnotationExpr(new Name("org.checkerframework.checker.nullness.qual.Nullable"))
                        )
                );
            }
        }

        final var parameter = new Parameter(parameterType, propertyName);
        parameter.addModifier(Modifier.Keyword.FINAL);

        method.addParameter(parameter);

        final var body = new BlockStmt();

        Expression varExpression = new NameExpr(propertyName);

        if (httpMethod == HttpMethod.PATCH) {
            varExpression = new MethodCallExpr(
                    new NameExpr("org.openapitools.jackson.nullable.JsonNullable"),
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
    }
}