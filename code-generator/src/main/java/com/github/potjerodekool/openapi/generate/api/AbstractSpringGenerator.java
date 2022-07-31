package com.github.potjerodekool.openapi.generate.api;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import com.github.potjerodekool.openapi.HttpMethod;
import com.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import com.github.potjerodekool.openapi.generate.CodeGenerator;
import com.github.potjerodekool.openapi.generate.JavaTypes;
import com.github.potjerodekool.openapi.generate.Types;
import com.github.potjerodekool.openapi.tree.OpenApiOperation;
import com.github.potjerodekool.openapi.tree.OpenApiParameter;
import com.github.potjerodekool.openapi.tree.OpenApiRequestBody;
import com.github.potjerodekool.openapi.tree.OpenApiResponse;
import com.github.potjerodekool.openapi.type.OpenApiType;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class AbstractSpringGenerator implements CodeGenerator {

    protected final Types types = new JavaTypes();

    private final String servletClassName;

    public AbstractSpringGenerator(final OpenApiGeneratorConfig config) {
        servletClassName = config.isUseJakartaServlet()
                ? "jakarta.servlet.http.HttpServletRequest"
                : "javax.servlet.http.HttpServletRequest";
    }

    protected List<Parameter> createParameters(final OpenApiOperation operation) {
        final var parameters = new ArrayList<>(operation.parameters().stream()
                .map(this::createParameter)
                .toList());

        final OpenApiRequestBody requestBody = operation.requestBody();

        if (requestBody != null) {
            final var bodyMediaType = ApiCodeGeneratorUtils.findJsonMediaType(requestBody.contentMediaType());
            final var bodyType = bodyMediaType != null
                    ? types.createType(bodyMediaType, false)
                    : types.createType("java.lang.Object");

            final var bodyParameter = new Parameter(bodyType, "body");

            if (shouldAddAnnotationsOnParameters()) {
                if (requestBody.required() != null) {
                    new NormalAnnotationExpr(
                            new Name("org.springframework.web.bind.annotation.RequestBody"),
                            NodeList.nodeList(
                                    new MemberValuePair(
                                            "required",
                                            new BooleanLiteralExpr(requestBody.required())
                                    )
                            )
                    );
                } else {
                    bodyParameter.addAnnotation(new MarkerAnnotationExpr("org.springframework.web.bind.annotation.RequestBody"));
                }
            }

            parameters.add(bodyParameter);
        }

        parameters.add(new Parameter(types.createType(servletClassName), "request"));

        return parameters;
    }

    protected boolean shouldAddAnnotationsOnParameters() {
        return true;
    }

    protected Parameter createParameter(final OpenApiParameter openApiParameter) {
        final var type = types.createType(openApiParameter.type(), Boolean.FALSE.equals(openApiParameter.required()));
        return new Parameter(
                type,
                openApiParameter.name()
        );
    }

    protected final Type getResponseType(final OpenApiResponse response) {
        final var contentMediaType = response.contentMediaType().get("application/json");

        if (contentMediaType == null) {
            return types.createType("java.lang.Void");
        }

        return types.createType(contentMediaType, false);
    }

    protected void processOperation(final HttpMethod httpMethod,
                                    final String path,
                                    final @Nullable OpenApiOperation operation,
                                    final ClassOrInterfaceDeclaration clazz) {
        if (operation == null) {
            return;
        }

        final var operationId = operation.operationId();

        if (operationId == null || "".equals(operationId)) {
            throw new IllegalArgumentException();
        }

        final var method = clazz.addMethod(operationId);
        createParameters(operation)
                .forEach(method::addParameter);

        final var responseTypes = resolveResponseTypes(httpMethod, operation);
        final Type responseType;

        if (responseTypes.isEmpty()) {
            responseType = new VoidType();
        } else if (responseTypes.size() == 1) {
            responseType = types.createType(
                    responseTypes.get(0),
                    true
            );
        } else {
            responseType = types.createType("java.lang.Object");
        }

        method.setType(responseType);

        postProcessOperation(
                httpMethod,
                path,
                operation,
                clazz,
                method);
    }

    protected List<OpenApiType> resolveResponseTypes(final HttpMethod httpMethod,
                                                     final OpenApiOperation operation) {
        final Map<String, OpenApiResponse> responses = operation.responses();

        final List<OpenApiType> responseTypes = new ArrayList<>();

        responses.values().forEach(response -> {
            final var contentMediaType = response.contentMediaType().get("application/json");

            if (contentMediaType != null) {
                responseTypes.add(contentMediaType);
            }
        });

        return responseTypes;
    }

    protected void postProcessOperation(final HttpMethod httpMethod,
                                        final String path,
                                        final OpenApiOperation operation,
                                        final ClassOrInterfaceDeclaration clazz,
                                        final MethodDeclaration method) {
    }

}
