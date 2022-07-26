package com.github.potjerodekool.openapi.generate.api;

import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MarkerAnnotationExpr;
import com.github.javaparser.ast.type.Type;
import com.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import com.github.potjerodekool.openapi.generate.CodeGenerator;
import com.github.potjerodekool.openapi.generate.JavaTypes;
import com.github.potjerodekool.openapi.generate.Types;
import com.github.potjerodekool.openapi.tree.OpenApiOperation;
import com.github.potjerodekool.openapi.tree.OpenApiParameter;
import com.github.potjerodekool.openapi.tree.OpenApiRequestBody;
import com.github.potjerodekool.openapi.tree.OpenApiResponse;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractSpringGenerator implements CodeGenerator {

    protected final Types types = new JavaTypes();

    private final String servletClassName;

    public AbstractSpringGenerator(final OpenApiGeneratorConfig config) {
        servletClassName = config.isUseJakartaServlet()
                ? "jakarta.servlet.http.HttpServletRequest"
                : "javax.servlet.http.HttpServletRequest";

    }

    // jakarta.servlet.http.HttpServletRequest

    protected List<Parameter> createParameters(final OpenApiOperation operation) {
        final var parameters = new ArrayList<Parameter>(operation.parameters().stream()
                .map(this::createParameter)
                .toList());

        final OpenApiRequestBody requestBody = operation.requestBody();

        if (requestBody != null) {
            final var bodyMediaType = ApiCodeGeneratorUtils.findJsonMediaType(requestBody.contentMediaType());
            final var bodyType = bodyMediaType != null
                    ? types.createType(bodyMediaType, false)
                    : types.createType("java.lang.Object");

            final var bodyParameter = new Parameter(bodyType, "body");
            bodyParameter.addAnnotation(new MarkerAnnotationExpr("org.springframework.web.bind.annotation.RequestBody"));

            parameters.add(bodyParameter);
        }

        parameters.add(new Parameter(types.createType(servletClassName), "request"));

        return parameters;
    }

    protected Parameter createParameter(final OpenApiParameter openApiParameter) {
        final var type = types.createType(openApiParameter.type(), openApiParameter.isNullable());
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


}
