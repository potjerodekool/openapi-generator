package io.github.potjerodekool.openapi.common.generate.service

import io.github.potjerodekool.codegen.Environment
import io.github.potjerodekool.codegen.model.element.ElementKind
import io.github.potjerodekool.codegen.model.element.Modifier
import io.github.potjerodekool.codegen.template.model.element.MethodElem
import io.github.potjerodekool.codegen.template.model.element.TypeElem
import io.github.potjerodekool.codegen.template.model.element.VariableElem
import io.github.potjerodekool.codegen.template.model.type.ClassOrInterfaceTypeExpr
import io.github.potjerodekool.codegen.template.model.type.NoTypeExpr
import io.github.potjerodekool.codegen.template.model.type.TypeExpr
import io.github.potjerodekool.codegen.template.model.type.WildCardTypeExpr
import io.github.potjerodekool.openapi.common.ApiConfiguration
import io.github.potjerodekool.openapi.common.GeneratorConfig
import io.github.potjerodekool.openapi.common.MissingOperationIdException
import io.github.potjerodekool.openapi.common.StatusCodes
import io.github.potjerodekool.openapi.common.generate.*
import io.github.potjerodekool.openapi.common.util.OpenApiUtils.findOkResponse
import io.github.potjerodekool.openapi.common.util.OpenApiUtils.getMediaType
import io.github.potjerodekool.openapi.common.util.OpenApiUtils.resolveResponseMediaType
import io.swagger.models.HttpMethod
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.responses.ApiResponse
import java.util.*
import java.util.function.Consumer

class ServiceApiGenerator(
    generatorConfig: GeneratorConfig?,
    apiConfiguration: ApiConfiguration?,
    environment: Environment?,
    typeUtils: OpenApiTypeUtils?
) : AbstractGenerator(generatorConfig!!, apiConfiguration!!, typeUtils!!, environment!!) {
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

        val clazz = findOrCreateClass(path, operation)

        val okReponseOptional: Optional<MutableMap.MutableEntry<String, ApiResponse>> = findOkResponse(operation.responses)
        val responseType = okReponseOptional
            .map { response: Map.Entry<String, ApiResponse> ->
                mapOkResponse(
                    response,
                    api,
                    method,
                    operation
                )
            }
            .orElseGet { NoTypeExpr.createVoidType() }

        val methodElem = MethodElem()
            .kind(ElementKind.METHOD)
            .returnType(responseType)
            .simpleName(operationId)

        createParameters(api, operation, method).forEach(Consumer { parameter: VariableElem? ->
            methodElem.parameter(
                parameter
            )
        })

        clazz.enclosedElement(methodElem)
    }

    override fun createParameters(
        api: OpenAPI,
        operation: Operation,
        httpMethod: HttpMethod
    ): MutableList<VariableElem> {
        val parameters = ArrayList(
            super.createParameters(api, operation, httpMethod)
        )

        val requestParameter = VariableElem()
            .kind(ElementKind.PARAMETER)
            .modifier(Modifier.FINAL)
            .type(ClassOrInterfaceTypeExpr("$basePackageName.Request"))
            .simpleName("request")
        parameters.add(requestParameter)
        return parameters
    }

    private fun mapOkResponse(
        okResponse: Map.Entry<String, ApiResponse>,
        api: OpenAPI,
        httpMethod: HttpMethod,
        operation: Operation
    ): TypeExpr {
        val responseType: TypeExpr

        val hasContent = (okResponse.value.content != null
                && !okResponse.value.content.isEmpty())

        if (hasContent) {
            val okResponseType = resolveResponseMediaType(okResponse.value.content)

            if (okResponseType != null) {
                val resolved = SchemaResolver.resolve(api, okResponseType.schema)
                val type = createType(api, resolved, okResponseType.extensions)
                responseType = if (type is WildCardTypeExpr
                ) type.expr as TypeExpr
                else type
            } else {
                responseType = if (okResponse.value.content
                        .containsKey("application/octet-stream")
                ) {
                    typeUtils.createType(
                        api,
                        null,
                        null,
                        null,
                        ContentType.APPLICATION_OCTET_STREAM,
                        true
                    )
                } else {
                    NoTypeExpr.createVoidType()
                }
            }
        } else if (httpMethod == HttpMethod.POST && StatusCodes.CREATED == okResponse.key) {
            val requestBody = operation.requestBody

            if (requestBody != null) {
                val pair = getMediaType(requestBody.content)
                val mediaType = pair!!.value

                responseType = typeUtils.createType(
                    api,
                    mediaType.schema,
                    null,
                    modelPackageName,
                    pair.key,
                    requestBody.required
                )
            } else {
                responseType = ClassOrInterfaceTypeExpr("java.lang.Object")
            }
        } else {
            responseType = NoTypeExpr.createVoidType()
        }

        return responseType
    }

    private fun createType(
        openAPI: OpenAPI,
        resolved: ResolvedSchemaResult,
        extensions: Map<String, Any>
    ): TypeExpr {
        val schema = resolved.schema

        val typeExpr = typeUtils.createType(
            openAPI,
            schema,
            extensions,
            modelPackageName,
            ContentType.JSON,
            null
        )

        if (typeExpr is ClassOrInterfaceTypeExpr
            && resolved.name != null
        ) {
            typeExpr.name(modelPackageName + "." + resolved.name)
        }

        return typeExpr
    }

    override fun createClass(simpleName: String?): TypeElem {
        return super.createClass(simpleName)
            .kind(ElementKind.INTERFACE)
    }

    override fun classNameSuffix(): String {
        return "ServiceApi"
    }
}
