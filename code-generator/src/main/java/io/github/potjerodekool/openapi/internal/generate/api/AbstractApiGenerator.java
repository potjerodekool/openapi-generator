package io.github.potjerodekool.openapi.internal.generate.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.expression.Expression;
import io.github.potjerodekool.codegen.model.tree.expression.FieldAccessExpression;
import io.github.potjerodekool.codegen.model.tree.java.JMethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.java.JVariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.NoTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.TypeExpression;
import io.github.potjerodekool.codegen.model.type.TypeKind;
import io.github.potjerodekool.openapi.*;
import io.github.potjerodekool.openapi.internal.ClassNames;
import io.github.potjerodekool.openapi.internal.generate.AbstractGenerator;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.ParameterAnnotationBuilder;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.media.SchemaAnnotationBuilder;
import io.github.potjerodekool.openapi.internal.generate.annotation.spring.web.*;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.tree.OpenApiOperation;
import io.github.potjerodekool.openapi.tree.OpenApiParameter;
import io.github.potjerodekool.openapi.tree.OpenApiPath;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static io.github.potjerodekool.openapi.internal.generate.OpenApiUtils.resolveResponseTypes;

public abstract class AbstractApiGenerator extends AbstractGenerator {

    private final OpenApiTypeUtils openApiTypeUtils;
    private final Map<String, String> controllers;
    private final String servletClassName;
    private final String validAnnotationClassName;

    protected AbstractApiGenerator(final GeneratorConfig generatorConfig,
                                   final Environment environment,
                                   final OpenApiTypeUtils openApiTypeUtils,
                                   final ApiConfiguration apiConfiguration) {
        super(
                generatorConfig,
                apiConfiguration,
                environment,
                openApiTypeUtils
        );
        this.openApiTypeUtils = openApiTypeUtils;
        this.controllers = apiConfiguration.controllers();

        this.servletClassName = generatorConfig.isFeatureEnabled(Features.FEATURE_JAKARTA)
                ? ClassNames.JAKARTA_HTTP_SERVLET_REQUEST
                : ClassNames.JAVA_HTTP_SERVLET_REQUEST;

        final var validationBasePackage = generatorConfig.isFeatureEnabled(Features.FEATURE_JAKARTA) ? "jakarta" : "javax";
        this.validAnnotationClassName = validationBasePackage + ".validation.Valid";
    }

    @Override
    protected void processOperation(final OpenApiPath openApiPath,
                                    final HttpMethod httpMethod,
                                    final String path,
                                    final @Nullable OpenApiOperation operation) {
        if (operation == null) {
            return;
        }

        final var operationId = operation.operationId();

        if (operationId == null || operationId.isEmpty()) {
            throw new MissingOperationIdException(path, httpMethod);
        }

        final var responseTypes = resolveResponseTypes(operation);
        final Expression responseType;

        if (responseTypes.isEmpty()) {
            responseType = new ClassOrInterfaceTypeExpression("java.lang.Void");
        } else if (responseTypes.size() == 1) {
            responseType = openApiTypeUtils.createTypeExpression(responseTypes.get(0));
        } else {
            responseType = new ClassOrInterfaceTypeExpression("java.lang.Object");
        }

        final var classDeclaration = findOrCreateClassDeclaration(openApiPath, operation);

        final var method = classDeclaration.addMethod(responseType, operationId, Set.of());
        createParameters(operation, httpMethod)
                .forEach(method::addParameter);
        afterProcessOperation(method);
        postProcessOperation(
                httpMethod,
                path,
                operation,
                method);

        addParametersDocumentation();
    }

    private void addParametersDocumentation() {
    }

    protected void afterProcessOperation(final JMethodDeclaration method) {
        final var responseType = method.getReturnType();

        final TypeExpression returnTypeArg;

        if (responseType instanceof NoTypeExpression noTypeExpression
            && noTypeExpression.getKind() == TypeKind.VOID) {
            returnTypeArg = new ClassOrInterfaceTypeExpression("java.lang.Void");
        } else {
            returnTypeArg = (TypeExpression) responseType;
        }

        final var returnType = new ClassOrInterfaceTypeExpression("org.springframework.http.ResponseEntity");
        returnType.addTypeArgument(returnTypeArg);
        method.setReturnType(returnType);
    }

    protected abstract void postProcessOperation(final HttpMethod httpMethod,
                                                 final String path,
                                                 final OpenApiOperation operation,
                                                 final JMethodDeclaration method);

    @Override
    protected String resolveClassName(final OpenApiPath openApiPath,
                                      final OpenApiOperation operation) {
        for (final var tag : operation.tags()) {
            final var className = this.controllers.get(tag);
            if (className != null) {
                return className;
            }
        }

        return super.resolveClassName(openApiPath, operation);
    }

    protected List<JVariableDeclaration> createParameters(final OpenApiOperation operation,
                                                          final HttpMethod httpMethod) {
        final var parameters = super.createParameters(operation, httpMethod);

        parameters.add(JVariableDeclaration.parameter()
                .modifier(Modifier.FINAL)
                .varType(new ClassOrInterfaceTypeExpression(servletClassName))
                .name("request")
        );

        addRequestBodyAnnotation(operation, parameters);
        return parameters;
    }

    private void addRequestBodyAnnotation(final OpenApiOperation operation,
                                          final List<JVariableDeclaration> parameters) {
        final var bodyParameterOptional = parameters.stream()
                .filter(parameter -> "body".equals(parameter.getName()))
                .findFirst();

        bodyParameterOptional.ifPresent(bodyParameter -> {
            final var requestBody = operation.requestBody();

            if (requestBody != null) {
                bodyParameter.annotation(new AnnotationExpression(validAnnotationClassName));
            }

            if (requestBody != null) {
                var bodyRequired = false;

                if (requestBody.required() != null) {
                    bodyRequired = requestBody.required();
                }

                bodyParameter.annotation(new RequestBodyAnnotationBuilder()
                        .required(bodyRequired)
                        .build());
            }
        });
    }

    protected JVariableDeclaration createParameter(final OpenApiParameter openApiParameter) {
        final var parameter = super.createParameter(openApiParameter);

        switch (openApiParameter.in()) {
            case PATH -> {
                parameter.annotation(createSpringPathVariableAnnotation(openApiParameter));
                parameter.annotation(createApiParamAnnotation(openApiParameter));
            }
            case QUERY -> {
                parameter.annotation(createSpringRequestParamAnnotation(openApiParameter));
                parameter.annotation(createApiParamAnnotation(openApiParameter));
            }
            case HEADER -> parameter.annotation(createSpringRequestHeaderAnnotation(openApiParameter));
            case COOKIE -> parameter.annotation(createSpringCookieValueAnnotation(openApiParameter));
        }

        return parameter;
    }

    private AnnotationExpression createSpringPathVariableAnnotation(final OpenApiParameter openApiParameter) {
        final var required = openApiParameter.required();

        final var pathVariableAnnotationBuilder = new PathVariableAnnotationBuilder()
                .name(openApiParameter.name());

        if (Boolean.FALSE.equals(required)) {
            pathVariableAnnotationBuilder.required(false);
        }

        return pathVariableAnnotationBuilder.build();
    }

    private AnnotationExpression createSpringRequestParamAnnotation(final OpenApiParameter openApiParameter) {
        final var required = openApiParameter.required();

        final var requestParamAnnotationBuilder = new RequestParamAnnotationBuilder()
                .name(openApiParameter.name());

        if (Boolean.FALSE.equals((required))) {
            requestParamAnnotationBuilder.required(false);
        }

        return requestParamAnnotationBuilder.build();
    }

    private AnnotationExpression createApiParamAnnotation(final OpenApiParameter openApiParameter) {
        final var required = openApiParameter.required();
        final var explode = openApiParameter.explode();
        final var allowEmptyValue = openApiParameter.allowEmptyValue();
        final var example = openApiParameter.example();
        final var description = openApiParameter.description();
        final var nullable = openApiParameter.nullable();

        final var parameterAnnotationBuilder = new ParameterAnnotationBuilder()
                .name(openApiParameter.name())
                .in(new FieldAccessExpression(
                                new ClassOrInterfaceTypeExpression("io.swagger.v3.oas.annotations.enums.ParameterIn"),
                                openApiParameter.in().name()
                        )
                )
                .description(description)
                .example(example);

        parameterAnnotationBuilder.required(required);

        if (Boolean.TRUE.equals(allowEmptyValue)) {
            parameterAnnotationBuilder.allowEmptyValue(true);
        }

        if (Boolean.TRUE.equals(explode)) {
            parameterAnnotationBuilder.explode(new FieldAccessExpression(
                    new ClassOrInterfaceTypeExpression("io.swagger.v3.oas.annotations.enums.Explode"),
                    "TRUE"
            ));
        }

        parameterAnnotationBuilder.schema(new SchemaAnnotationBuilder().nullable(nullable).build());

        return parameterAnnotationBuilder.build();
    }

    private AnnotationExpression createSpringRequestHeaderAnnotation(final OpenApiParameter openApiParameter) {
        final var required = openApiParameter.required();

        return new RequestHeaderAnnotationBuilder()
                .name(openApiParameter.name())
                .required(required)
                .build();
    }

    private AnnotationExpression createSpringCookieValueAnnotation(final OpenApiParameter openApiParameter) {
        return new CookieValueAnnotationBuilder()
                .name(openApiParameter.name())
                .required(openApiParameter.required())
                .build();
    }
}
