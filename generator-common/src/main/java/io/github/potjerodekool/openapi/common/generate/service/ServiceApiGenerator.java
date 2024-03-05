package io.github.potjerodekool.openapi.common.generate.service;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.template.model.element.MethodElem;
import io.github.potjerodekool.codegen.template.model.element.TypeElem;
import io.github.potjerodekool.codegen.template.model.element.VariableElem;
import io.github.potjerodekool.codegen.template.model.type.ClassOrInterfaceTypeExpr;
import io.github.potjerodekool.codegen.template.model.type.NoTypeExpr;
import io.github.potjerodekool.codegen.template.model.type.TypeExpr;
import io.github.potjerodekool.codegen.template.model.type.WildCardTypeExpr;
import io.github.potjerodekool.openapi.common.ApiConfiguration;
import io.github.potjerodekool.openapi.common.GeneratorConfig;
import io.github.potjerodekool.openapi.common.MissingOperationIdException;
import io.github.potjerodekool.openapi.common.StatusCodes;
import io.github.potjerodekool.openapi.common.generate.ContentTypes;
import io.github.potjerodekool.openapi.common.generate.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.common.generate.AbstractGenerator;
import io.github.potjerodekool.openapi.common.generate.ResolvedSchemaResult;
import io.github.potjerodekool.openapi.common.generate.SchemaResolver;
import io.github.potjerodekool.openapi.common.util.OpenApiUtils;
import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ServiceApiGenerator extends AbstractGenerator {

    public ServiceApiGenerator(final GeneratorConfig generatorConfig,
                               final ApiConfiguration apiConfiguration,
                               final Environment environment,
                               final OpenApiTypeUtils typeUtils) {
        super(generatorConfig, apiConfiguration, typeUtils, environment);
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

        final var clazz = findOrCreateClass(path, operation);

        final var okReponseOptional = OpenApiUtils.findOkResponse(operation.getResponses());
        final TypeExpr responseType = okReponseOptional
                .map(response -> mapOkResponse(
                        response,
                        api,
                        httpMethod,
                        operation
                ))
                .orElseGet(NoTypeExpr::createVoidType);

        final var method = new MethodElem()
                .kind(ElementKind.METHOD)
                .returnType(responseType)
                .simpleName(operationId);

        createParameters(api, operation, httpMethod).forEach(method::parameter);

        clazz.enclosedElement(method);
    }

    @Override
    protected List<VariableElem> createParameters(final OpenAPI api,
                                                  final Operation operation,
                                                  final HttpMethod httpMethod) {

        final var parameters = new ArrayList<>(
                super.createParameters(api, operation, httpMethod)
        );

        final var requestParameter = new VariableElem()
                .kind(ElementKind.PARAMETER)
                .modifier(Modifier.FINAL)
                .type(new ClassOrInterfaceTypeExpr(getBasePackageName() + ".Request"))
                .simpleName("request");
        parameters.add(requestParameter);
        return parameters;
    }

    private TypeExpr mapOkResponse(final Map.Entry<String, ApiResponse> okResponse,
                                   final OpenAPI api,
                                   final HttpMethod httpMethod,
                                   final Operation operation) {
        final TypeExpr responseType;

        final var hasContent = okResponse.getValue().getContent() != null
                && !okResponse.getValue().getContent().isEmpty();

        if (hasContent) {
            final var okResponseType = OpenApiUtils.resolveResponseMediaType(okResponse.getValue().getContent());

            if (okResponseType != null) {
                final var resolved = SchemaResolver.resolve(api, okResponseType);
                final var type = createType(api, resolved);
                responseType = type instanceof WildCardTypeExpr wildCardTypeExpr
                        ? (TypeExpr) wildCardTypeExpr.getExpr()
                        : type;
            } else {
                responseType = NoTypeExpr.createVoidType();
            }
        } else if (httpMethod == HttpMethod.POST
                && StatusCodes.CREATED.equals(okResponse.getKey())) {
            final var requestBody = operation.getRequestBody();

            if (requestBody != null) {
                final var jsonContent = requestBody.getContent().get(ContentTypes.JSON);
                responseType = getTypeUtils().createType(
                        api,
                        jsonContent.getSchema(),
                        getModelPackageName(),
                        ContentTypes.JSON,
                        requestBody.getRequired()
                );
            } else {
                responseType = new ClassOrInterfaceTypeExpr("java.lang.Object");
            }
        } else {
            responseType = NoTypeExpr.createVoidType();
        }

        return responseType;
    }

    private TypeExpr createType(final OpenAPI openAPI,
                                final ResolvedSchemaResult resolved) {
        final var schema = resolved.schema();

        if (schema instanceof ObjectSchema || schema instanceof ComposedSchema) {
            return new ClassOrInterfaceTypeExpr(getModelPackageName() + "." + resolved.name());
        } else {
            return getTypeUtils().createType(
                    openAPI,
                    schema,
                    getModelPackageName(),
                    ContentTypes.JSON,
                    null
            );
        }
    }

    @Override
    protected TypeElem createClass(final String simpleName) {
        return super.createClass(simpleName)
                .kind(ElementKind.INTERFACE);
    }

    @Override
    protected String classNameSuffix() {
        return "ServiceApi";
    }
}
