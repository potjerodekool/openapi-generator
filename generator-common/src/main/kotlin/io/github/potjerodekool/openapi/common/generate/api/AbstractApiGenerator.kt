package io.github.potjerodekool.openapi.common.generate.api

import io.github.potjerodekool.codegen.Environment
import io.github.potjerodekool.codegen.model.element.ElementKind
import io.github.potjerodekool.codegen.model.element.Modifier
import io.github.potjerodekool.codegen.model.type.TypeKind
import io.github.potjerodekool.codegen.template.model.annotation.Annot
import io.github.potjerodekool.codegen.template.model.element.MethodElem
import io.github.potjerodekool.codegen.template.model.element.VariableElem
import io.github.potjerodekool.codegen.template.model.expression.FieldAccessExpr
import io.github.potjerodekool.codegen.template.model.expression.IdentifierExpr
import io.github.potjerodekool.codegen.template.model.type.ClassOrInterfaceTypeExpr
import io.github.potjerodekool.codegen.template.model.type.NoTypeExpr
import io.github.potjerodekool.codegen.template.model.type.TypeExpr
import io.github.potjerodekool.openapi.common.*
import io.github.potjerodekool.openapi.common.ParameterLocation.Companion.parseIn
import io.github.potjerodekool.openapi.common.generate.AbstractGenerator
import io.github.potjerodekool.openapi.common.generate.ContentType
import io.github.potjerodekool.openapi.common.generate.OpenApiTypeUtils
import io.github.potjerodekool.openapi.common.generate.SchemaResolver
import io.github.potjerodekool.openapi.common.generate.annotation.openapi.ParameterAnnotationBuilder
import io.github.potjerodekool.openapi.common.generate.annotation.openapi.media.SchemaAnnotationBuilder
import io.github.potjerodekool.openapi.common.util.OpenApiUtils.findOkResponse
import io.github.potjerodekool.openapi.common.util.OpenApiUtils.getMediaType
import io.github.potjerodekool.openapi.common.util.OpenApiUtils.resolveResponseTypes
import io.swagger.models.HttpMethod
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import java.util.*
import java.util.function.Consumer

abstract class AbstractApiGenerator protected constructor(
    generatorConfig: GeneratorConfig,
    apiConfiguration: ApiConfiguration,
    typeUtils: OpenApiTypeUtils,
    environment: Environment
) : AbstractGenerator(generatorConfig, apiConfiguration, typeUtils, environment) {
    private val servletClassName = ClassNames.JAKARTA_HTTP_SERVLET_REQUEST
    private val validAnnotationClassName = "jakarta.validation.Valid"

    override fun visitOperation(
        api: OpenAPI,
        method: HttpMethod,
        path: String,
        operation: Operation?,
        apiConfiguration: ApiConfiguration
    ) {
        if (operation == null) {
            return
        }

        val operationId = operation.operationId

        if (operationId == null
            || operationId.isEmpty()
        ) {
            throw MissingOperationIdException(path, method)
        }

        val responseTypes = resolveResponseTypes(operation)
        val responseType: TypeExpr

        if (responseTypes.isEmpty()) {
            val okResponseOptional: Optional<MutableMap.MutableEntry<String, ApiResponse>> = findOkResponse(operation.responses)
                .filter { okResponse: Map.Entry<String, ApiResponse> -> "201" != okResponse.key }
                .filter { okResponse: MutableMap.MutableEntry<String, ApiResponse> ->
                    (okResponse.value.content != null
                            && okResponse.value.content.containsKey("application/octet-stream"))
                }

            responseType = okResponseOptional.map {
                typeUtils.createType(
                    api,
                    null,
                    null,
                    null,
                    ContentType.APPLICATION_OCTET_STREAM,
                    true
                )
            }.orElseGet { ClassOrInterfaceTypeExpr("java.lang.Void") }
        } else if (responseTypes.size == 1) {
            val schemaAndExtensions = responseTypes.first()

            responseType = typeUtils.createType(
                api,
                schemaAndExtensions.schema,
                schemaAndExtensions.extensions,
                modelPackageName,
                null,
                null
            )
        } else {
            responseType = ClassOrInterfaceTypeExpr("java.lang.Object")
        }

        val clazz = findOrCreateClass(path, operation)

        val methodElem = MethodElem()
            .kind(ElementKind.METHOD)
            .returnType(responseType)
            .simpleName(operationId)

        clazz.enclosedElement(methodElem)

        createParameters(api, operation, method)
            .forEach(Consumer { parameter: VariableElem? -> methodElem.parameter(parameter) })
        afterProcessOperation(methodElem)
        postProcessOperation(
            api,
            method,
            path,
            operation,
            methodElem
        )

        addParametersDocumentation()
    }

    private fun addParametersDocumentation() {
    }

    private fun afterProcessOperation(method: MethodElem) {
        val responseType = method.returnType

        val returnTypeArg = if (responseType is NoTypeExpr
            && responseType.getTypeKind() == TypeKind.VOID
        ) {
            ClassOrInterfaceTypeExpr("java.lang.Void")
        } else {
            responseType
        }

        val returnType = ClassOrInterfaceTypeExpr("org.springframework.http.ResponseEntity")
        returnType.typeArgument(returnTypeArg)
        method.returnType(returnType)
    }

    protected abstract fun postProcessOperation(
        openAPI: OpenAPI?,
        httpMethod: HttpMethod?,
        path: String?,
        operation: Operation?,
        method: MethodElem?
    )

    override fun createParameters(
        api: OpenAPI,
        operation: Operation,
        httpMethod: HttpMethod
    ): MutableList<VariableElem> {
        val parameters = super.createParameters(api, operation, httpMethod)

        parameters.add(
            VariableElem()
                .kind(ElementKind.PARAMETER)
                .modifier(Modifier.FINAL)
                .type(ClassOrInterfaceTypeExpr(servletClassName))
                .simpleName("request")
        )

        addValidAnnotation(api, operation, parameters)
        return parameters
    }

    private fun addValidAnnotation(
        openAPI: OpenAPI,
        operation: Operation,
        parameters: List<VariableElem>
    ) {
        val bodyParameterOptional = parameters.stream()
            .filter { parameter: VariableElem -> "body" == parameter.simpleName }
            .findFirst()

        bodyParameterOptional.ifPresent { bodyParameter: VariableElem ->
            val requestBody = operation.requestBody
            if (shouldValidateRequestBody(openAPI, requestBody)) {
                bodyParameter.annotation(Annot(validAnnotationClassName))
            }
        }
    }

    private fun shouldValidateRequestBody(
        openAPI: OpenAPI,
        requestBody: RequestBody?
    ): Boolean {
        if (requestBody == null) {
            return false
        }

        val content = getMediaType(requestBody.content) ?: return false

        val mediaType = content.value

        val resolved = SchemaResolver.resolve(openAPI, mediaType.schema)
        return resolved.schema is ObjectSchema
    }

    override fun createParameter(
        openAPI: OpenAPI,
        openApiParameter: Parameter
    ): VariableElem {
        val parameter = super.createParameter(openAPI, openApiParameter)
        val `in` = parseIn(openApiParameter.getIn())

        if (`in` == ParameterLocation.PATH) {
            parameter.annotation(createApiParamAnnotation(openApiParameter))
        } else if (`in` == ParameterLocation.QUERY) {
            parameter.annotation(createApiParamAnnotation(openApiParameter))
        }

        return parameter
    }

    private fun createApiParamAnnotation(openApiParameter: Parameter): Annot {
        val required = openApiParameter.required
        val explode = openApiParameter.explode
        val allowEmptyValue = openApiParameter.allowEmptyValue
        val example = openApiParameter.example as String?
        val description = openApiParameter.description
        val nullable = openApiParameter.schema.nullable

        val parameterAnnotationBuilder = ParameterAnnotationBuilder()
            .name(openApiParameter.name)
            .`in`(
                FieldAccessExpr()
                    .target(ClassOrInterfaceTypeExpr("io.swagger.v3.oas.annotations.enums.ParameterIn"))
                    .field(IdentifierExpr(openApiParameter.getIn().uppercase(Locale.getDefault())))
            )
            .description(description)
            .example(example)

        parameterAnnotationBuilder.required(required)

        if (java.lang.Boolean.TRUE == allowEmptyValue) {
            parameterAnnotationBuilder.allowEmptyValue(true)
        }

        if (java.lang.Boolean.TRUE == explode) {
            parameterAnnotationBuilder.explode(
                FieldAccessExpr()
                    .target(ClassOrInterfaceTypeExpr("io.swagger.v3.oas.annotations.enums.Explode"))
                    .field(IdentifierExpr("TRUE"))
            )
        }

        parameterAnnotationBuilder.schema(SchemaAnnotationBuilder().nullable(nullable).build())

        return parameterAnnotationBuilder.build()
    }
}
