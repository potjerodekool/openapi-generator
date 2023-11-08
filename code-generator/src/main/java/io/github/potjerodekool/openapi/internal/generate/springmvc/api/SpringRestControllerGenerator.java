package io.github.potjerodekool.openapi.internal.generate.springmvc.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.expression.*;
import io.github.potjerodekool.codegen.model.tree.java.JMethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.BlockStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ReturnStatement;
import io.github.potjerodekool.codegen.model.tree.statement.java.JClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.java.JVariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.TypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.VarTypeExpression;
import io.github.potjerodekool.openapi.ApiConfiguration;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.internal.StatusCodes;
import io.github.potjerodekool.openapi.internal.generate.ContentTypes;
import io.github.potjerodekool.openapi.internal.generate.api.AbstractApiGenerator;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.tree.OpenApiOperation;
import io.github.potjerodekool.openapi.tree.OpenApiPath;
import io.github.potjerodekool.openapi.tree.OpenApiResponse;

import java.util.*;

import static io.github.potjerodekool.openapi.internal.generate.OpenApiUtils.findOkResponse;

public class SpringRestControllerGenerator extends AbstractApiGenerator {

    public SpringRestControllerGenerator(final GeneratorConfig generatorConfig,
                                         final ApiConfiguration apiConfiguration,
                                         final Environment environment,
                                         final OpenApiTypeUtils openApiTypeUtils) {
        super(generatorConfig, environment, openApiTypeUtils, apiConfiguration);
    }

    @Override
    protected String generateClasName(final OpenApiPath openApiPath,
                                      final OpenApiOperation operation) {
        return super.generateClasName(openApiPath, operation) + "Controller";
    }

    @Override
    protected JClassDeclaration createClass(final String packageName,
                                            final Name simpleName) {
        final var classDeclaration = new JClassDeclaration(simpleName, ElementKind.CLASS)
                .modifier(Modifier.PUBLIC);

        classDeclaration.annotation(new AnnotationExpression("javax.annotation.processing.Generated", LiteralExpression.createStringLiteralExpression(getClass().getName())));
        classDeclaration.annotation("org.springframework.web.bind.annotation.RestController");
        classDeclaration.annotation("org.springframework.web.bind.annotation.CrossOrigin");

        final var name = simpleName.toString();
        final var separatorIndex = name.lastIndexOf("Controller");
        final var apiName = name.substring(0, separatorIndex) + "Api";
        classDeclaration.addImplement(new ClassOrInterfaceTypeExpression(packageName + "." +apiName));

        final var serviceName = name.substring(0, separatorIndex) + "ServiceApi";

        final var serviceField = new JVariableDeclaration(
                ElementKind.FIELD,
                Set.of(Modifier.PRIVATE, Modifier.FINAL),
                new ClassOrInterfaceTypeExpression(serviceName),
                "service",
                null,
                null
        );

        classDeclaration.addEnclosed(serviceField);

        final var constructor = classDeclaration.addConstructor();
        constructor.addModifier(Modifier.PUBLIC);

        constructor.addParameter(new JVariableDeclaration(
                ElementKind.PARAMETER,
                Set.of(Modifier.FINAL),
                new ClassOrInterfaceTypeExpression(serviceName),
                "service",
                null,
                null
        ));

        final var constructorBody = new BlockStatement(new BinaryExpression(
                new FieldAccessExpression(
                        new IdentifierExpression("this"),
                        "service"
                ),
                new IdentifierExpression("service"),
                Operator.ASSIGN)
        );
        constructor.setBody(constructorBody);

        return classDeclaration;
    }

    @Override
    protected void postProcessOperation(final HttpMethod httpMethod,
                                        final String path,
                                        final OpenApiOperation operation,
                                        final JMethodDeclaration method) {
        method.annotation("java.lang.Override");
        method.addModifier(Modifier.PUBLIC);

        final var okResponseOptional = findOkResponse(operation.responses());

        final List<Expression> arguments = new ArrayList<>(operation.parameters().stream()
                .map(parameter -> new IdentifierExpression(parameter.name()))
                .toList());

        if (operation.requestBody() != null) {
            arguments.add(new IdentifierExpression("body"));
        }

        arguments.add(
            new NewClassExpression(
                    new ClassOrInterfaceTypeExpression(getBasePackageName() + ".HttpServletRequestWrapper"),
                    List.of(new IdentifierExpression("request"))
            )
        );

        final BlockStatement body = okResponseOptional
                .map(okResponse -> generateOkResponse(
                        httpMethod,
                        operation,
                        okResponse,
                        arguments))
                .orElseGet(this::generateNotImplemented);

        method.setBody(body);
    }

    private BlockStatement generateOkResponse(final HttpMethod httpMethod,
                                              final OpenApiOperation operation,
                                              final Map.Entry<String, OpenApiResponse> okResponse,
                                              final List<Expression> arguments) {
        final BlockStatement body = new BlockStatement();

        final var statusCode = okResponse.getKey();
        final var response = okResponse.getValue();
        final var hasContent = !response.contentMediaType().isEmpty();
        final var requestBody = operation.requestBody();
        final TypeExpression requestBodyType;

        if (requestBody != null) {
            final var jsonContent = requestBody.contentMediaType().get(ContentTypes.JSON);
            requestBodyType = jsonContent != null
                    ? getOpenApiTypeUtils().createTypeExpression(jsonContent.schema())
                    : null;
        } else {
            requestBodyType = null;
        }

        final var serviceMethodCall = new MethodCallExpression(
                new IdentifierExpression("service"),
                operation.operationId(),
                arguments
        );

        final var isCreateRequest = httpMethod == HttpMethod.POST
                && StatusCodes.CREATED.equals(okResponse.getKey());

        if (hasContent || isCreateRequest) {
            final var variable = new JVariableDeclaration(
                    ElementKind.LOCAL_VARIABLE,
                    Set.of(Modifier.FINAL),
                    new VarTypeExpression(),
                    "result",
                    serviceMethodCall,
                    null
            );

            body.add(variable);
        } else {
            body.add(serviceMethodCall);
        }

        MethodCallExpression methodCall;

        if (StatusCodes.CREATED.equals(statusCode)) {
            final Expression idExpression = requestBodyType != null
                ? new MethodCallExpression(new IdentifierExpression("result"), "getId")
                : LiteralExpression.createNullLiteralExpression();

            final var locationVar = new JVariableDeclaration(
                    ElementKind.LOCAL_VARIABLE,
                    Set.of(Modifier.FINAL),
                    new VarTypeExpression(),
                    "location",
                    new MethodCallExpression(
                            new ClassOrInterfaceTypeExpression(
                                    getBasePackageName() + ".ApiUtils"
                            ),
                            "createLocation",
                            List.of(
                                    new IdentifierExpression("request"),
                                    idExpression
                            )
                    ),
                    null
            );

            body.add(locationVar);

            methodCall = new MethodCallExpression(
                    new ClassOrInterfaceTypeExpression("org.springframework.http.ResponseEntity"),
                    "created",
                    List.of(new IdentifierExpression("location"))
            );
        } else if (StatusCodes.NO_CONTENT.equals(statusCode)) {
            methodCall = new MethodCallExpression(
                    new ClassOrInterfaceTypeExpression("org.springframework.http.ResponseEntity"),
                    "noContent"
            );
        } else {
            methodCall = new MethodCallExpression(
                    new ClassOrInterfaceTypeExpression("org.springframework.http.ResponseEntity"),
                    "status",
                    List.of(LiteralExpression.createIntLiteralExpression(Integer.parseInt(statusCode)))
            );
        }

        if (!hasContent) {
            methodCall = new MethodCallExpression(
                    methodCall,
                    "build"
            );
        } else {
            methodCall = new MethodCallExpression(
                    methodCall,
                    "body",
                    List.of(new IdentifierExpression("result"))
            );
        }

        body.add(new ReturnStatement(methodCall));
        return body;
    }

    private BlockStatement generateNotImplemented() {
        final BlockStatement body = new BlockStatement();

        body.add(new ReturnStatement(
                new MethodCallExpression(
                        new MethodCallExpression(
                                new ClassOrInterfaceTypeExpression("org.springframework.http.ResponseEntity"),
                                "status",
                                List.of(new FieldAccessExpression(
                                        new ClassOrInterfaceTypeExpression("org.springframework.http.HttpStatus"),
                                        "NOT_IMPLEMENTED"
                                ))
                        ),
                        "build"
                )
        ));

        return body;
    }

}
