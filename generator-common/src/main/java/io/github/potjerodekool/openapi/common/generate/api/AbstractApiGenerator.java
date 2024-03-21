package io.github.potjerodekool.openapi.common.generate.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.type.TypeKind;
import io.github.potjerodekool.codegen.template.model.annotation.Annot;
import io.github.potjerodekool.codegen.template.model.element.MethodElem;
import io.github.potjerodekool.codegen.template.model.element.VariableElem;
import io.github.potjerodekool.codegen.template.model.expression.FieldAccessExpr;
import io.github.potjerodekool.codegen.template.model.expression.IdentifierExpr;
import io.github.potjerodekool.codegen.template.model.type.ClassOrInterfaceTypeExpr;
import io.github.potjerodekool.codegen.template.model.type.NoTypeExpr;
import io.github.potjerodekool.codegen.template.model.type.TypeExpr;
import io.github.potjerodekool.openapi.common.ApiConfiguration;
import io.github.potjerodekool.openapi.common.GeneratorConfig;
import io.github.potjerodekool.openapi.common.MissingOperationIdException;
import io.github.potjerodekool.openapi.common.ParameterLocation;
import io.github.potjerodekool.openapi.common.ClassNames;
import io.github.potjerodekool.openapi.common.generate.ContentTypes;
import io.github.potjerodekool.openapi.common.generate.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.common.generate.AbstractGenerator;
import io.github.potjerodekool.openapi.common.generate.annotation.openapi.ParameterAnnotationBuilder;
import io.github.potjerodekool.openapi.common.generate.annotation.openapi.media.SchemaAnnotationBuilder;
import io.github.potjerodekool.openapi.common.generate.SchemaResolver;
import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;

import java.util.List;
import java.util.Map;

import static io.github.potjerodekool.openapi.common.util.OpenApiUtils.findOkResponse;
import static io.github.potjerodekool.openapi.common.util.OpenApiUtils.resolveResponseTypes;

public abstract class AbstractApiGenerator extends AbstractGenerator {

    private final Map<String, String> controllers;
    private final String servletClassName;
    private final String validAnnotationClassName;

    protected AbstractApiGenerator(final GeneratorConfig generatorConfig,
                                   final ApiConfiguration apiConfiguration,
                                   final OpenApiTypeUtils typeUtils,
                                   final Environment environment) {
        super(generatorConfig, apiConfiguration, typeUtils, environment);

        this.controllers = apiConfiguration.controllers();
        this.servletClassName = ClassNames.JAKARTA_HTTP_SERVLET_REQUEST;
        this.validAnnotationClassName = "jakarta.validation.Valid";
    }

    @Override
    public void visitOperation(final OpenAPI api,
                               final HttpMethod httpMethod,
                               final String path,
                               final Operation operation) {
        if (operation == null) {
            return;
        }

        final var operationId = operation.getOperationId();

        if (operationId == null
                || operationId.isEmpty()) {
            throw new MissingOperationIdException(path, httpMethod);
        }

        final var responseTypes = resolveResponseTypes(operation);
        final TypeExpr responseType;

        if (responseTypes.isEmpty()) {
            final var okResponseOptional = findOkResponse(operation.getResponses())
                    .filter(okResponse -> !"201".equals(okResponse.getKey()))
                    .filter(okResponse ->
                            okResponse.getValue().getContent() != null
                                    && okResponse.getValue().getContent().containsKey("application/octet-stream")
                    );

            responseType = okResponseOptional.map(
                    okResponse -> getTypeUtils().createType(
                            api,
                            null,
                            null,
                            null,
                            "application/octet-stream",
                            true
                    )
            ).orElseGet(() -> new ClassOrInterfaceTypeExpr("java.lang.Void"));
        } else if (responseTypes.size() == 1) {
            final var schemaAndExtensions = responseTypes.getFirst();

            responseType = getTypeUtils().createType(
                    api,
                    schemaAndExtensions.schema(),
                    schemaAndExtensions.extensions(),
                    getModelPackageName(),
                    null,
                    null
            );
        } else {
            responseType = new ClassOrInterfaceTypeExpr("java.lang.Object");
        }

        final var clazz = findOrCreateClass(path, operation);

        final var method = new MethodElem()
                .kind(ElementKind.METHOD)
                .returnType(responseType)
                .simpleName(operationId);

        clazz.enclosedElement(method);

        createParameters(api, operation, httpMethod)
                .forEach(method::parameter);
        afterProcessOperation(method);
        postProcessOperation(
                api,
                httpMethod,
                path,
                operation,
                method);

        addParametersDocumentation();
    }

    private void addParametersDocumentation() {
    }

    protected void afterProcessOperation(final MethodElem method) {
        final var responseType = method.getReturnType();

        final TypeExpr returnTypeArg;

        if (responseType instanceof NoTypeExpr noTypeExpr
                && noTypeExpr.getTypeKind() == TypeKind.VOID) {
            returnTypeArg = new ClassOrInterfaceTypeExpr("java.lang.Void");
        } else {
            returnTypeArg = responseType;
        }

        final var returnType = new ClassOrInterfaceTypeExpr("org.springframework.http.ResponseEntity");
        returnType.typeArgument(returnTypeArg);
        method.returnType(returnType);
    }

    protected abstract void postProcessOperation(final OpenAPI openAPI,
                                                 final HttpMethod httpMethod,
                                                 final String path,
                                                 final Operation operation,
                                                 final MethodElem method);

    @Override
    protected String resolveClassName(final String path, final Operation operation) {
        final var tags = operation.getTags();

        if (tags != null) {
            for (final var tag : tags) {
                final var className = this.controllers.get(tag);
                if (className != null) {
                    return className;
                }
            }
        }

        return super.resolveClassName(path, operation);
    }

    @Override
    protected List<VariableElem> createParameters(final OpenAPI openAPI,
                                                  final Operation operation,
                                                  final HttpMethod httpMethod) {
        final var parameters = super.createParameters(openAPI, operation, httpMethod);

        parameters.add(new VariableElem()
                .kind(ElementKind.PARAMETER)
                .modifier(Modifier.FINAL)
                .type(new ClassOrInterfaceTypeExpr(servletClassName))
                .simpleName("request")
        );

        addValidAnnotation(openAPI, operation, parameters);
        return parameters;
    }

    private void addValidAnnotation(final OpenAPI openAPI,
                                    final Operation operation,
                                    final List<VariableElem> parameters) {
        final var bodyParameterOptional = parameters.stream()
                .filter(parameter -> "body".equals(parameter.getSimpleName()))
                .findFirst();

        bodyParameterOptional.ifPresent(bodyParameter -> {
            final var requestBody = operation.getRequestBody();

            if (shouldValidateRequestBody(openAPI, requestBody)) {
                bodyParameter.annotation(new Annot(validAnnotationClassName));
            }
        });
    }

    private boolean shouldValidateRequestBody(final OpenAPI openAPI,
                                              final RequestBody requestBody) {
        if (requestBody == null) {
            return false;
        }

        final var content = requestBody.getContent().get(ContentTypes.JSON);

        if (content == null) {
            return false;
        }

        final var resolved = SchemaResolver.resolve(openAPI, content.getSchema());
        return resolved.schema() instanceof ObjectSchema;
    }

    @Override
    protected VariableElem createParameter(final OpenAPI openAPI,
                                           final Parameter openApiParameter) {
        final var parameter = super.createParameter(openAPI, openApiParameter);
        final var in = ParameterLocation.parseIn(openApiParameter.getIn());

        if (in == ParameterLocation.PATH) {
            parameter.annotation(createApiParamAnnotation(openApiParameter));
        } else if (in == ParameterLocation.QUERY) {
            parameter.annotation(createApiParamAnnotation(openApiParameter));
        }

        return parameter;
    }

    private Annot createApiParamAnnotation(final Parameter openApiParameter) {
        final var required = openApiParameter.getRequired();
        final var explode = openApiParameter.getExplode();
        final var allowEmptyValue = openApiParameter.getAllowEmptyValue();
        final var example = (String) openApiParameter.getExample();
        final var description = openApiParameter.getDescription();
        final var nullable = openApiParameter.getSchema().getNullable();

        final var parameterAnnotationBuilder = new ParameterAnnotationBuilder()
                .name(openApiParameter.getName())
                .in(new FieldAccessExpr()
                        .target(new ClassOrInterfaceTypeExpr("io.swagger.v3.oas.annotations.enums.ParameterIn"))
                        .field(new IdentifierExpr(openApiParameter.getIn().toUpperCase()))
                )
                .description(description)
                .example(example);

        parameterAnnotationBuilder.required(required);

        if (Boolean.TRUE.equals(allowEmptyValue)) {
            parameterAnnotationBuilder.allowEmptyValue(true);
        }

        if (Boolean.TRUE.equals(explode)) {
            parameterAnnotationBuilder.explode(new FieldAccessExpr()
                    .target(new ClassOrInterfaceTypeExpr("io.swagger.v3.oas.annotations.enums.Explode"))
                    .field(new IdentifierExpr("TRUE"))
            );
        }

        parameterAnnotationBuilder.schema(new SchemaAnnotationBuilder().nullable(nullable).build());

        return parameterAnnotationBuilder.build();
    }

}
