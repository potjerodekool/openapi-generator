package io.github.potjerodekool.openapi.internal.generate.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.MethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.expression.Expression;
import io.github.potjerodekool.codegen.model.tree.expression.FieldAccessExpression;
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
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
import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;

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
    protected void processOperation(final OpenAPI openAPI,
                                    final PathItem openApiPath,
                                    final HttpMethod httpMethod,
                                    final String path,
                                    final @Nullable Operation operation) {
        if (operation == null) {
            return;
        }

        final var operationId = operation.getOperationId();

        if (operationId == null || operationId.isEmpty()) {
            throw new MissingOperationIdException(path, httpMethod);
        }

        final var responseTypes = resolveResponseTypes(operation);
        final Expression responseType;

        if (responseTypes.isEmpty()) {
            responseType = new ClassOrInterfaceTypeExpression("java.lang.Void");
        } else if (responseTypes.size() == 1) {
            responseType = openApiTypeUtils.createTypeExpression(responseTypes.getFirst(), openAPI);
        } else {
            responseType = new ClassOrInterfaceTypeExpression("java.lang.Object");
        }

        final var classDeclaration = findOrCreateClassDeclaration(path, openApiPath, operation);

        final var method = classDeclaration.createMethod()
                .returnType(responseType)
                .simpleName(Name.of(operationId));
        createParameters(openAPI, operation, httpMethod)
                .forEach(method::parameter);
        afterProcessOperation(method);
        postProcessOperation(
                openAPI,
                httpMethod,
                path,
                operation,
                method);

        addParametersDocumentation();
    }

    private void addParametersDocumentation() {
    }

    protected void afterProcessOperation(final MethodDeclaration method) {
        final var responseType = method.getReturnType();

        final TypeExpression returnTypeArg;

        if (responseType instanceof NoTypeExpression noTypeExpression
                && noTypeExpression.getKind() == TypeKind.VOID) {
            returnTypeArg = new ClassOrInterfaceTypeExpression("java.lang.Void");
        } else {
            returnTypeArg = (TypeExpression) responseType;
        }

        final var returnType = new ClassOrInterfaceTypeExpression("org.springframework.http.ResponseEntity");
        returnType.typeArgument(returnTypeArg);
        method.returnType(returnType);
    }

    protected abstract void postProcessOperation(final OpenAPI openAPI,
                                                 final HttpMethod httpMethod,
                                                 final String path,
                                                 final Operation operation,
                                                 final MethodDeclaration method);

    @Override
    protected String resolveClassName(final String path,
                                      final PathItem openApiPath,
                                      final Operation operation) {
        final var tags = operation.getTags();

        if (tags != null) {
            for (final var tag : tags) {
                final var className = this.controllers.get(tag);
                if (className != null) {
                    return className;
                }
            }
        }

        return super.resolveClassName(path, openApiPath, operation);
    }

    @Override
    protected List<VariableDeclaration> createParameters(final OpenAPI openAPI,
                                                         final Operation operation,
                                                         final HttpMethod httpMethod) {
        final var parameters = super.createParameters(openAPI, operation, httpMethod);

        parameters.add(new VariableDeclaration()
                .kind(ElementKind.PARAMETER)
                .modifier(Modifier.FINAL)
                .varType(new ClassOrInterfaceTypeExpression(servletClassName))
                .name("request")
        );

        addRequestBodyAnnotation(operation, parameters);
        return parameters;
    }

    private void addRequestBodyAnnotation(final Operation operation,
                                          final List<VariableDeclaration> parameters) {
        final var bodyParameterOptional = parameters.stream()
                .filter(parameter -> "body".equals(parameter.getName()))
                .findFirst();

        bodyParameterOptional.ifPresent(bodyParameter -> {
            final var requestBody = operation.getRequestBody();

            if (requestBody != null) {
                bodyParameter.annotation(new AnnotationExpression(validAnnotationClassName));
            }

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
    }

    @Override
    protected VariableDeclaration createParameter(final OpenAPI openAPI,
                                                  final Parameter openApiParameter) {
        final var parameter = super.createParameter(openAPI, openApiParameter);
        final var in = ParameterLocation.parseIn(openApiParameter.getIn());

        switch (in) {
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

    private AnnotationExpression createSpringPathVariableAnnotation(final Parameter openApiParameter) {
        final var required = openApiParameter.getRequired();

        final var pathVariableAnnotationBuilder = new PathVariableAnnotationBuilder()
                .name(openApiParameter.getName());

        if (Boolean.FALSE.equals(required)) {
            pathVariableAnnotationBuilder.required(false);
        }

        return pathVariableAnnotationBuilder.build();
    }

    private AnnotationExpression createSpringRequestParamAnnotation(final Parameter openApiParameter) {
        final var required = openApiParameter.getRequired();

        final var requestParamAnnotationBuilder = new RequestParamAnnotationBuilder()
                .name(openApiParameter.getName());

        if (Boolean.FALSE.equals((required))) {
            requestParamAnnotationBuilder.required(false);
        }

        return requestParamAnnotationBuilder.build();
    }

    private AnnotationExpression createApiParamAnnotation(final Parameter openApiParameter) {
        final var required = openApiParameter.getRequired();
        final var explode = openApiParameter.getExplode();
        final var allowEmptyValue = openApiParameter.getAllowEmptyValue();
        final var example = (String) openApiParameter.getExample();
        final var description = openApiParameter.getDescription();
        final var nullable = openApiParameter.getSchema().getNullable();

        final var parameterAnnotationBuilder = new ParameterAnnotationBuilder()
                .name(openApiParameter.getName())
                .in(new FieldAccessExpression(
                                new ClassOrInterfaceTypeExpression("io.swagger.v3.oas.annotations.enums.ParameterIn"),
                                openApiParameter.getIn()
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

    private AnnotationExpression createSpringRequestHeaderAnnotation(final Parameter openApiParameter) {
        final var required = openApiParameter.getRequired();

        return new RequestHeaderAnnotationBuilder()
                .name(openApiParameter.getName())
                .required(required)
                .build();
    }

    private AnnotationExpression createSpringCookieValueAnnotation(final Parameter openApiParameter) {
        return new CookieValueAnnotationBuilder()
                .name(openApiParameter.getName())
                .required(openApiParameter.getRequired())
                .build();
    }
}
