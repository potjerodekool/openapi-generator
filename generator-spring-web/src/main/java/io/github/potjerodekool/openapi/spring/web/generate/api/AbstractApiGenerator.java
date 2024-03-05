package io.github.potjerodekool.openapi.spring.web.generate.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.template.model.annotation.Annot;
import io.github.potjerodekool.codegen.template.model.element.VariableElem;
import io.github.potjerodekool.openapi.common.ApiConfiguration;
import io.github.potjerodekool.openapi.common.GeneratorConfig;
import io.github.potjerodekool.openapi.common.ParameterLocation;
import io.github.potjerodekool.openapi.common.generate.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.spring.web.generate.annotation.spring.web.*;
import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.parameters.Parameter;

import java.util.List;

public abstract class AbstractApiGenerator extends io.github.potjerodekool.openapi.common.generate.api.AbstractApiGenerator {
    protected AbstractApiGenerator(final GeneratorConfig generatorConfig,
                                   final ApiConfiguration apiConfiguration,
                                   final OpenApiTypeUtils typeUtils,
                                   final Environment environment) {
        super(generatorConfig, apiConfiguration, typeUtils, environment);
    }

    @Override
    protected VariableElem createParameter(final OpenAPI openAPI,
                                           final Parameter openApiParameter) {
        final var parameter = super.createParameter(openAPI, openApiParameter);
        final var in = ParameterLocation.parseIn(openApiParameter.getIn());

        switch (in) {
            case PATH -> parameter.annotation(createSpringPathVariableAnnotation(openApiParameter));
            case QUERY -> parameter.annotation(createSpringRequestParamAnnotation(openApiParameter));
            case HEADER -> parameter.annotation(createSpringRequestHeaderAnnotation(openApiParameter));
            case COOKIE -> parameter.annotation(createSpringCookieValueAnnotation(openApiParameter));
        }

        return parameter;
    }

    @Override
    protected List<VariableElem> createParameters(final OpenAPI openAPI, final Operation operation, final HttpMethod httpMethod) {
        final var parameters = super.createParameters(openAPI, operation, httpMethod);

        final var bodyParameterOptional = parameters.stream()
                .filter(parameter -> "body".equals(parameter.getSimpleName()))
                .findFirst();

        bodyParameterOptional.ifPresent(bodyParameter -> {
            final var requestBody = operation.getRequestBody();

            if (requestBody != null) {
                var bodyRequired = false;

                if (requestBody.getRequired() != null) {
                    bodyRequired = requestBody.getRequired();
                }

                bodyParameter.annotation(new RequestBodyAnnotationBuilder()
                        .required(bodyRequired)
                        .build());
            }
        });

        return parameters;
    }

    private Annot createSpringPathVariableAnnotation(final Parameter openApiParameter) {
        final var required = openApiParameter.getRequired();

        final var pathVariableAnnotationBuilder = new PathVariableAnnotationBuilder()
                .name(openApiParameter.getName());

        if (Boolean.FALSE.equals(required)) {
            pathVariableAnnotationBuilder.required(false);
        }

        return pathVariableAnnotationBuilder.build();
    }

    private Annot createSpringRequestParamAnnotation(final Parameter openApiParameter) {
        final var required = openApiParameter.getRequired();

        final var requestParamAnnotationBuilder = new RequestParamAnnotationBuilder()
                .name(openApiParameter.getName());

        if (Boolean.FALSE.equals((required))) {
            requestParamAnnotationBuilder.required(false);
        }

        return requestParamAnnotationBuilder.build();
    }

    private Annot createSpringRequestHeaderAnnotation(final Parameter openApiParameter) {
        final var required = openApiParameter.getRequired();

        return new RequestHeaderAnnotationBuilder()
                .name(openApiParameter.getName())
                .required(required)
                .build();
    }

    private Annot createSpringCookieValueAnnotation(final Parameter openApiParameter) {
        return new CookieValueAnnotationBuilder()
                .name(openApiParameter.getName())
                .required(openApiParameter.getRequired())
                .build();
    }
}
