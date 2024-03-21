package io.github.potjerodekool.openapi.spring.web.generate.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.tree.expression.Operator;
import io.github.potjerodekool.codegen.template.model.annotation.Annot;
import io.github.potjerodekool.codegen.template.model.element.MethodElem;
import io.github.potjerodekool.codegen.template.model.element.TypeElem;
import io.github.potjerodekool.codegen.template.model.element.VariableElem;
import io.github.potjerodekool.codegen.template.model.expression.*;
import io.github.potjerodekool.codegen.template.model.statement.BlockStm;
import io.github.potjerodekool.codegen.template.model.statement.ReturnStm;
import io.github.potjerodekool.codegen.template.model.statement.VariableDeclarationStm;
import io.github.potjerodekool.codegen.template.model.type.ClassOrInterfaceTypeExpr;
import io.github.potjerodekool.codegen.template.model.type.TypeExpr;
import io.github.potjerodekool.codegen.template.model.type.VarTypeExp;
import io.github.potjerodekool.openapi.common.ApiConfiguration;
import io.github.potjerodekool.openapi.common.GeneratorConfig;
import io.github.potjerodekool.openapi.common.StatusCodes;
import io.github.potjerodekool.openapi.common.generate.ContentTypes;
import io.github.potjerodekool.openapi.common.generate.OpenApiTypeUtils;
import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.github.potjerodekool.openapi.common.util.OpenApiUtils.findOkResponse;

public class SpringRestControllerGenerator extends AbstractApiGenerator {
    public SpringRestControllerGenerator(final GeneratorConfig generatorConfig,
                                         final ApiConfiguration apiConfiguration,
                                         final OpenApiTypeUtils typeUtils,
                                         final Environment environment) {
        super(generatorConfig, apiConfiguration, typeUtils, environment);
    }

    @Override
    protected String classNameSuffix() {
        return "Controller";
    }

    @Override
    protected TypeElem createClass(final String simpleName) {
        final var clazz = super.createClass(simpleName);
        clazz.kind(ElementKind.CLASS);

        clazz.annotation(new Annot("org.springframework.web.bind.annotation.RestController"));
        clazz.annotation(new Annot("org.springframework.web.bind.annotation.CrossOrigin"));

        final var separatorIndex = simpleName.lastIndexOf("Controller");
        final var apiName = simpleName.substring(0, separatorIndex) + "Api";
        final var packageName = getBasePackageName();

        clazz.implement(new ClassOrInterfaceTypeExpr(packageName + "." + apiName));

        final var serviceName = packageName + "." + simpleName.substring(0, separatorIndex) + "ServiceApi";

        final var serviceField = new VariableElem()
                .kind(ElementKind.FIELD)
                .modifiers(Modifier.PRIVATE, Modifier.FINAL)
                .type(new ClassOrInterfaceTypeExpr(serviceName))
                .simpleName("service");

        clazz.enclosedElement(serviceField);

        final var constructor = clazz.createConstructor();
        constructor.modifier(Modifier.PUBLIC);

        constructor.parameter(new VariableElem()
                .kind(ElementKind.PARAMETER)
                .modifier(Modifier.FINAL)
                .type(new ClassOrInterfaceTypeExpr(serviceName))
                .simpleName("service"));

        final var constructorBody = new BlockStm()
                .statement(
                        new BinaryExpr()
                                .left(
                                        new FieldAccessExpr()
                                                .target(new IdentifierExpr("this"))
                                                .field(new IdentifierExpr("service"))
                                )
                                .operator(Operator.ASSIGN)
                                .right(new IdentifierExpr("service"))
                );
        constructor.body(constructorBody);

        return clazz;
    }

    @Override
    protected void postProcessOperation(final OpenAPI openAPI,
                                        final HttpMethod httpMethod,
                                        final String path,
                                        final Operation operation,
                                        final MethodElem method) {
        method.annotation(new Annot("java.lang.Override"));
        method.modifier(Modifier.PUBLIC);

        final var okResponseOptional = findOkResponse(operation.getResponses());
        final List<Expr> arguments = new ArrayList<>();

        if (operation.getParameters() != null) {
            arguments.addAll(operation.getParameters().stream()
                    .map(parameter -> new IdentifierExpr(parameter.getName()))
                    .toList());
        }

        if (operation.getRequestBody() != null) {
            arguments.add(new IdentifierExpr("body"));
        }

        arguments.add(
                new NewClassExpr()
                        .name(getBasePackageName() + ".HttpServletRequestWrapper")
                        .arguments(List.of(new IdentifierExpr("request")))
        );

        final BlockStm body = okResponseOptional
                .map(okResponse -> generateOkResponse(
                        openAPI,
                        httpMethod,
                        operation,
                        okResponse,
                        arguments))
                .orElseGet(this::generateNotImplemented);

        method.body(body);
    }

    private BlockStm generateOkResponse(final OpenAPI openAPI, final HttpMethod httpMethod,
                                        final Operation operation,
                                        final Map.Entry<String, ApiResponse> okResponse,
                                        final List<Expr> arguments) {
        final var body = new BlockStm();

        final var statusCode = okResponse.getKey();
        final var response = okResponse.getValue();
        final var hasContent = response.getContent() != null
                && !response.getContent().isEmpty();
        final var requestBody = operation.getRequestBody();
        final TypeExpr requestBodyType;

        if (requestBody != null) {
            final var jsonContent = requestBody.getContent().get(ContentTypes.JSON);
            requestBodyType = jsonContent != null
                    ? getTypeUtils().createType(
                    openAPI,
                    jsonContent.getSchema(),
                    null,
                    getModelPackageName(),
                    ContentTypes.JSON,
                    requestBody.getRequired())
                    : null;
        } else {
            requestBodyType = null;
        }

        final var serviceMethodCall = new MethodInvocationExpr()
                .target(new IdentifierExpr("service"))
                .name(operation.getOperationId())
                .arguments(arguments);

        final var isCreateRequest = httpMethod == HttpMethod.POST
                && StatusCodes.CREATED.equals(okResponse.getKey());

        final var jsonResponse =
                response.getContent() != null
                        ? response.getContent().get(ContentTypes.JSON)
                        : null;

        //Check if the response has an id property
        final boolean responseWithId = jsonResponse != null && hasIdProperty(jsonResponse.getSchema());

        if (hasContent || isCreateRequest) {
            final var variable = new VariableDeclarationStm()
                    .modifier(Modifier.FINAL)
                    .type(new VarTypeExp())
                    .identifier("result")
                    .initExpression(serviceMethodCall);

            body.statement(variable);
        } else {
            body.statement(serviceMethodCall);
        }

        MethodInvocationExpr methodCall;

        //Only call created method if response has a single id property
        if (StatusCodes.CREATED.equals(statusCode) && responseWithId) {
            final Expr idExpression = requestBodyType != null
                    ? new MethodInvocationExpr()
                    .target(new IdentifierExpr("result"))
                    .name("getId")
                    : new SimpleLiteralExpr(null);

            final var locationVar = new VariableDeclarationStm()
                    .modifier(Modifier.FINAL)
                    .type(new VarTypeExp())
                    .identifier("location")
                    .initExpression(new MethodInvocationExpr()
                            .target(new ClassOrInterfaceTypeExpr(getBasePackageName() + ".ApiUtils"))
                            .name("createLocation")
                            .arguments(
                                    new IdentifierExpr("request"),
                                    idExpression
                            )

                    );

            body.statement(locationVar);

            methodCall = new MethodInvocationExpr()
                    .target(new ClassOrInterfaceTypeExpr("org.springframework.http.ResponseEntity"))
                    .name("created")
                    .argument(new IdentifierExpr("location"));
        } else if (StatusCodes.NO_CONTENT.equals(statusCode)) {
            methodCall = new MethodInvocationExpr()
                    .target(new ClassOrInterfaceTypeExpr("org.springframework.http.ResponseEntity"))
                    .name("noContent");
        } else {
            methodCall = new MethodInvocationExpr()
                    .target(new ClassOrInterfaceTypeExpr("org.springframework.http.ResponseEntity"))
                    .name("status")
                    .argument(new SimpleLiteralExpr(Integer.parseInt(statusCode)));
        }

        if (!hasContent) {
            methodCall = new MethodInvocationExpr()
                    .target(methodCall)
                    .name("build");
        } else {
            methodCall = new MethodInvocationExpr()
                    .target(methodCall)
                    .name("body")
                    .argument(new IdentifierExpr("result"));
        }

        body.statement(new ReturnStm(methodCall));
        return body;
    }

    private boolean hasIdProperty(final Schema<?> schema) {
        if (schema.getAllOf() != null) {
            if (schema.getAllOf().stream()
                    .anyMatch(this::hasIdProperty)) {
                return true;
            }
        }

        return schema.getProperties() != null && schema.getProperties().containsKey("id");
    }

    private BlockStm generateNotImplemented() {
        final var body = new BlockStm();

        body.statement(new ReturnStm(
                new MethodInvocationExpr()
                        .target(
                                new MethodInvocationExpr()
                                        .target(new ClassOrInterfaceTypeExpr("org.springframework.http.ResponseEntity"))
                                        .name("status")
                                        .argument(
                                                new FieldAccessExpr()
                                                        .target(new ClassOrInterfaceTypeExpr("org.springframework.http.HttpStatus"))
                                                        .field(new IdentifierExpr("NOT_IMPLEMENTED"))
                                        )
                        ).name("build")
        ));

        return body;
    }
}
