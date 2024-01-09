package io.github.potjerodekool.openapi.internal.generate.springmvc.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.expression.*;
import io.github.potjerodekool.codegen.model.tree.MethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.BlockStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ReturnStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.TypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.VarTypeExpression;
import io.github.potjerodekool.openapi.ApiConfiguration;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.internal.StatusCodes;
import io.github.potjerodekool.openapi.internal.generate.ContentTypes;
import io.github.potjerodekool.openapi.internal.generate.api.AbstractApiGenerator;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;
import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.responses.ApiResponse;

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
    protected String generateClasName(final String path,
                                      final PathItem openApiPath,
                                      final Operation operation) {
        return super.generateClasName(path, openApiPath, operation) + "Controller";
    }

    @Override
    protected ClassDeclaration createClass(final String packageName,
                                            final Name simpleName) {
        final var classDeclaration = new ClassDeclaration()
                .simpleName(Name.of(simpleName))
                .kind(ElementKind.CLASS)
                .modifier(Modifier.PUBLIC);

        classDeclaration.annotation(new AnnotationExpression("javax.annotation.processing.Generated", LiteralExpression.createStringLiteralExpression(getClass().getName())));
        classDeclaration.annotation("org.springframework.web.bind.annotation.RestController");
        classDeclaration.annotation("org.springframework.web.bind.annotation.CrossOrigin");

        final var name = simpleName.toString();
        final var separatorIndex = name.lastIndexOf("Controller");
        final var apiName = name.substring(0, separatorIndex) + "Api";
        classDeclaration.addImplement(new ClassOrInterfaceTypeExpression(packageName + "." +apiName));

        final var serviceName = packageName + "." + name.substring(0, separatorIndex) + "ServiceApi";

        final var serviceField = new VariableDeclaration()
                .kind(ElementKind.FIELD)
                .modifiers(Modifier.PRIVATE, Modifier.FINAL)
                .varType(new ClassOrInterfaceTypeExpression(serviceName))
                .name("service");

        classDeclaration.addEnclosed(serviceField);

        final var constructor = classDeclaration.createConstructor();
        constructor.modifier(Modifier.PUBLIC);

        constructor.parameter(new VariableDeclaration()
                        .kind(ElementKind.PARAMETER)
                        .modifier(Modifier.FINAL)
                        .varType(new ClassOrInterfaceTypeExpression(serviceName))
                        .name("service"));

        final var constructorBody = new BlockStatement(new BinaryExpression(
                new FieldAccessExpression(
                        new IdentifierExpression("this"),
                        "service"
                ),
                new IdentifierExpression("service"),
                Operator.ASSIGN)
        );
        constructor.body(constructorBody);

        return classDeclaration;
    }

    @Override
    protected void postProcessOperation(final OpenAPI openAPI,
                                        final HttpMethod httpMethod,
                                        final String path,
                                        final Operation operation,
                                        final MethodDeclaration method) {
        method.annotation("java.lang.Override");
        method.modifier(Modifier.PUBLIC);

        final var okResponseOptional = findOkResponse(operation.getResponses());
        final List<Expression> arguments = new ArrayList<>();

        if (operation.getParameters() != null) {
            arguments.addAll(operation.getParameters().stream()
                    .map(parameter -> new IdentifierExpression(parameter.getName()))
                    .toList());
        }

        if (operation.getRequestBody() != null) {
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
                        openAPI,
                        httpMethod,
                        operation,
                        okResponse,
                        arguments))
                .orElseGet(this::generateNotImplemented);

        method.body(body);
    }

    private BlockStatement generateOkResponse(final OpenAPI openAPI, final HttpMethod httpMethod,
                                              final Operation operation,
                                              final Map.Entry<String, ApiResponse> okResponse,
                                              final List<Expression> arguments) {
        final BlockStatement body = new BlockStatement();

        final var statusCode = okResponse.getKey();
        final var response = okResponse.getValue();
        final var hasContent = response.getContent() != null
                && !response.getContent().isEmpty();
        final var requestBody = operation.getRequestBody();
        final TypeExpression requestBodyType;

        if (requestBody != null) {
            final var jsonContent = requestBody.getContent().get(ContentTypes.JSON);
            requestBodyType = jsonContent != null
                    ? getOpenApiTypeUtils().createTypeExpression(jsonContent.getSchema(), openAPI)
                    : null;
        } else {
            requestBodyType = null;
        }

        final var serviceMethodCall = new MethodCallExpression(
                new IdentifierExpression("service"),
                operation.getOperationId(),
                arguments
        );

        final var isCreateRequest = httpMethod == HttpMethod.POST
                && StatusCodes.CREATED.equals(okResponse.getKey());

        if (hasContent || isCreateRequest) {
            final var variable = new VariableDeclaration()
                    .kind(ElementKind.LOCAL_VARIABLE)
                    .modifier(Modifier.FINAL)
                    .varType(new VarTypeExpression())
                    .name("result")
                    .initExpression(serviceMethodCall);

            body.add(variable);
        } else {
            body.add(serviceMethodCall);
        }

        MethodCallExpression methodCall;

        if (StatusCodes.CREATED.equals(statusCode)) {
            final Expression idExpression = requestBodyType != null
                ? new MethodCallExpression(new IdentifierExpression("result"), "getId")
                : LiteralExpression.createNullLiteralExpression();

            final var locationVar = new VariableDeclaration()
                    .kind(ElementKind.LOCAL_VARIABLE)
                    .modifier(Modifier.FINAL)
                    .varType(new VarTypeExpression())
                    .name("location")
                    .initExpression(new MethodCallExpression(
                            new ClassOrInterfaceTypeExpression(
                                    getBasePackageName() + ".ApiUtils"
                            ),
                            "createLocation",
                            List.of(
                                    new IdentifierExpression("request"),
                                    idExpression
                            )
                    ));

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
