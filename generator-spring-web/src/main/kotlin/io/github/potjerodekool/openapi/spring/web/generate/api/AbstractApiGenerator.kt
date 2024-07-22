package io.github.potjerodekool.openapi.spring.web.generate.api

import io.github.potjerodekool.codegen.Environment
import io.github.potjerodekool.codegen.model.util.QualifiedName
import io.github.potjerodekool.codegen.template.model.annotation.Annot
import io.github.potjerodekool.codegen.template.model.element.MethodElem
import io.github.potjerodekool.codegen.template.model.element.VariableElem
import io.github.potjerodekool.codegen.template.model.expression.ArrayExpr
import io.github.potjerodekool.codegen.template.model.expression.Expr
import io.github.potjerodekool.codegen.template.model.expression.SimpleLiteralExpr
import io.github.potjerodekool.codegen.template.model.type.ArrayTypeExpr
import io.github.potjerodekool.codegen.template.model.type.ClassOrInterfaceTypeExpr
import io.github.potjerodekool.codegen.template.model.type.TypeExpr
import io.github.potjerodekool.openapi.common.ApiConfiguration
import io.github.potjerodekool.openapi.common.GeneratorConfig
import io.github.potjerodekool.openapi.common.ParameterLocation
import io.github.potjerodekool.openapi.common.ParameterLocation.Companion.parseIn
import io.github.potjerodekool.openapi.common.generate.ContentType
import io.github.potjerodekool.openapi.common.generate.ContentType.Companion.fromValue
import io.github.potjerodekool.openapi.common.generate.OpenApiTypeUtils
import io.github.potjerodekool.openapi.common.generate.annotation.GeneralAnnotationBuilder
import io.github.potjerodekool.openapi.common.generate.annotation.OperationAnnotationBuilder
import io.github.potjerodekool.openapi.common.generate.annotation.openapi.header.HeaderAnnotationBuilder
import io.github.potjerodekool.openapi.common.generate.annotation.openapi.media.*
import io.github.potjerodekool.openapi.common.generate.annotation.openapi.parameter.RequestBodyAnnotationBuilder
import io.github.potjerodekool.openapi.common.generate.annotation.openapi.response.ApiResponseAnnotationBuilder
import io.github.potjerodekool.openapi.common.generate.annotation.openapi.response.ApiResponsesAnnotationBuilder
import io.github.potjerodekool.openapi.common.generate.annotation.openapi.security.SecurityRequirementAnnotationBuilder
import io.github.potjerodekool.openapi.common.generate.annotation.openapi.security.SecurityRequirementsBuilder
import io.github.potjerodekool.openapi.common.generate.api.AbstractApiGenerator
import io.github.potjerodekool.openapi.common.util.CollectionBuilder
import io.github.potjerodekool.openapi.common.util.CollectionUtils.nonNull
import io.github.potjerodekool.openapi.spring.web.generate.annotation.spring.web.*
import io.swagger.models.HttpMethod
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.headers.Header
import io.swagger.v3.oas.models.media.*
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.security.SecurityRequirement
import java.util.*
import java.util.function.Consumer
import kotlin.Char
import kotlin.IllegalStateException
import kotlin.String
import kotlin.UnsupportedOperationException

abstract class AbstractApiGenerator protected constructor(
    generatorConfig: GeneratorConfig?,
    apiConfiguration: ApiConfiguration?,
    typeUtils: OpenApiTypeUtils?,
    environment: Environment?
) : AbstractApiGenerator(
    generatorConfig!!, apiConfiguration!!, typeUtils!!, environment!!
) {
    override fun createParameter(
        openAPI: OpenAPI,
        openApiParameter: Parameter
    ): VariableElem {
        val parameter = super.createParameter(openAPI, openApiParameter)
        val `in` = parseIn(openApiParameter.getIn())

        when (`in`) {
            ParameterLocation.PATH -> parameter.annotation(createSpringPathVariableAnnotation(openApiParameter))
            ParameterLocation.QUERY -> parameter.annotation(createSpringRequestParamAnnotation(openApiParameter))
            ParameterLocation.HEADER -> parameter.annotation(createSpringRequestHeaderAnnotation(openApiParameter))
            ParameterLocation.COOKIE -> parameter.annotation(createSpringCookieValueAnnotation(openApiParameter))
        }
        val defaultValue = openApiParameter.schema.default

        if (defaultValue != null) {
            parameter.findAnnotationByName("io.swagger.v3.oas.annotations.Parameter")
                .ifPresent { paramAnnotation: Annot ->
                    val schemaAnnotation = paramAnnotation.attributes["schema"] as Annot?
                    schemaAnnotation?.attribute("defaultValue", SimpleLiteralExpr(defaultValue.toString()))
                }
        }

        return parameter
    }

    override fun createParameters(
        api: OpenAPI,
        operation: Operation,
        httpMethod: HttpMethod
    ): MutableList<VariableElem> {
        val parameters = super.createParameters(api, operation, httpMethod)
        annotatedRequestParts(operation, parameters)
        return parameters
    }

    override fun createBodyParameter(
        operation: Operation?,
        bodyType: TypeExpr
    ): VariableElem {
        val bodyParameter = super.createBodyParameter(operation, bodyType)

        val requestBody = operation!!.requestBody

        var bodyRequired = false

        if (requestBody.required != null) {
            bodyRequired = requestBody.required
        }

        bodyParameter.annotation(
            io.github.potjerodekool.openapi.spring.web.generate.annotation.spring.web.RequestBodyAnnotationBuilder()
                .required(bodyRequired)
                .build()
        )

        return bodyParameter
    }

    private fun annotatedRequestParts(
        operation: Operation,
        parameters: List<VariableElem>
    ) {
        val requestBody = operation.requestBody ?: return

        val bodyMediaType = requestBody.content[ContentType.MULTIPART_FORM_DATA.type]
            ?: return

        val bodySchema = bodyMediaType.schema as? ObjectSchema ?: return

        val requiredProperties = Objects.requireNonNullElse(bodySchema.required, listOf())
        val propertyNames: Set<String> = bodySchema.properties.keys

        propertyNames.forEach(Consumer { propertyName: String ->
            val parameter = parameters.stream()
                .filter { param: VariableElem -> param.simpleName == propertyName }
                .findFirst()
            parameter.ifPresent { param: VariableElem ->
                val annotationBuilder =
                    GeneralAnnotationBuilder("org.springframework.web.bind.annotation.RequestPart")
                if (requiredProperties.contains(propertyName)) {
                    annotationBuilder.add("required", true)
                }
                param.annotation(annotationBuilder.build())
            }
        })
    }

    private fun createSpringPathVariableAnnotation(openApiParameter: Parameter): Annot {
        val required = openApiParameter.required

        val pathVariableAnnotationBuilder = PathVariableAnnotationBuilder()
            .name(openApiParameter.name)

        if (required == true) {
            pathVariableAnnotationBuilder.required(false)
        }

        return pathVariableAnnotationBuilder.build()
    }

    private fun createSpringRequestParamAnnotation(openApiParameter: Parameter): Annot {
        val required = openApiParameter.required

        val requestParamAnnotationBuilder = RequestParamAnnotationBuilder()
            .name(openApiParameter.name)

        if (required == false) {
            requestParamAnnotationBuilder.required(false)
        }

        return requestParamAnnotationBuilder.build()
    }

    private fun createSpringRequestHeaderAnnotation(openApiParameter: Parameter): Annot {
        val required = openApiParameter.required

        return RequestHeaderAnnotationBuilder()
            .name(openApiParameter.name)
            .required(required)
            .build()
    }

    private fun createSpringCookieValueAnnotation(openApiParameter: Parameter): Annot {
        return CookieValueAnnotationBuilder()
            .name(openApiParameter.name)
            .required(openApiParameter.required)
            .build()
    }

    protected fun addMethodAnnotations(
        openAPI: OpenAPI,
        httpMethod: HttpMethod?,
        path: String,
        operation: Operation,
        method: MethodElem
    ) {
        method.annotation(
            OperationAnnotationBuilder()
                .summary(operation.summary)
                .operationId(operation.operationId)
                .tags(operation.tags)
                .requestBody(createRequestBody(openAPI, httpMethod, operation.requestBody))
                .build()
        )

        val securityRequirements = operation.security

        if (securityRequirements != null) {
            val securityRequirementsAnnotation = createSecurityRequirementsAnnotation(securityRequirements)
            if (securityRequirementsAnnotation != null) {
                method.annotation(securityRequirementsAnnotation)
            }
        }

        val apiResponsesAnnotation = createApiResponsesAnnotation(openAPI, operation)

        method.annotation(apiResponsesAnnotation)
        method.annotation(createMappingAnnotation(httpMethod, path, operation))
    }

    private fun createRequestBody(
        openAPI: OpenAPI,
        httpMethod: HttpMethod?,
        openApiRequestBody: RequestBody?
    ): Annot? {
        if (openApiRequestBody == null) {
            return null
        }

        val requestBodyBuilder = RequestBodyAnnotationBuilder()

        val contentList = openApiRequestBody.content.entries.stream()
            .map { entry: Map.Entry<String?, MediaType?> ->
                createContentAnnotation(
                    openAPI, httpMethod, fromValue(
                        entry.key!!
                    ), entry.value
                )
            }
            .toList()

        requestBodyBuilder.content(contentList)
        return requestBodyBuilder.build()
    }

    private fun createContentAnnotation(
        openAPI: OpenAPI,
        httpMethod: HttpMethod?,
        mediaType: ContentType?,
        content: MediaType?
    ): Annot {
        val contentAnnotationBuilder = ContentAnnotationBuilder()

        if (content != null) {
            val schema = content.schema

            val schemaType = typeUtils.createType(
                openAPI,
                schema,
                null,
                modelPackageName,
                mediaType,
                null
            )

            if (httpMethod == HttpMethod.PATCH) {
                var classType = schemaType as ClassOrInterfaceTypeExpr

                if (schema !is ArraySchema) {
                    if (classType.typeArguments != null && classType.typeArguments.size == 1) {
                        classType = classType.typeArguments.first() as ClassOrInterfaceTypeExpr
                    }

                    val qualifiedName = QualifiedName.from(classType.name)
                    var simpleName = qualifiedName.simpleName().toString()
                    val packageName = qualifiedName.packageName().toString()

                    if (!simpleName.contains("Patch")) {
                        simpleName = "Patch$simpleName"
                    }
                    classType.name("$packageName.$simpleName")
                }
            }

            val examples: MutableList<Expr> = ArrayList()

            if (content.examples != null) {
                examples.addAll(content.examples.entries.stream()
                    .map { exampleEntry: Map.Entry<String, Example> ->
                        createExampleObject(
                            exampleEntry.key,
                            exampleEntry.value
                        )
                    }
                    .toList())
            }

            contentAnnotationBuilder.mediaType(mediaType!!.stringValue())
            contentAnnotationBuilder.examples<Expr>(examples)

            if (schema is ArraySchema) {
                val elementType = getElementType(schemaType)
                contentAnnotationBuilder.array(createArrayAnnotation(openAPI, schema, elementType))
            } else if (schema is MapSchema) {
                val schemaPropertyAnnotation = SchemaPropertyAnnotationBuilder()
                    .name("additionalProp1")
                    .build()

                val additionalProperties = schema.getAdditionalProperties()
                    ?: throw IllegalStateException("Missing additionalProperties")

                if (additionalProperties is Schema<*>) {
                    var typeName: String? = additionalProperties.type

                    if (typeName == null && additionalProperties is ObjectSchema) {
                        typeName = "object"
                    }

                    val format: String = additionalProperties.format
                    contentAnnotationBuilder.schemaProperties(schemaPropertyAnnotation)
                    contentAnnotationBuilder.additionalPropertiesSchema(
                        SchemaAnnotationBuilder()
                            .type(typeName)
                            .format(format)
                            .build()
                    )
                } else {
                    //TODO handle Boolean
                    throw UnsupportedOperationException()
                }
            } else {
                if (schemaType is ArrayTypeExpr) {
                    contentAnnotationBuilder.array(createArrayAnnotation(openAPI, schema, getElementType(schemaType)))
                } else {
                    contentAnnotationBuilder.schema(
                        createSchemaAnnotation(schema)
                            .implementation(typeUtils.resolveImplementationType(openAPI, schemaType))
                            .requiredMode(false)
                            .build()
                    )
                }
            }
        } else {
            contentAnnotationBuilder.schema(
                SchemaAnnotationBuilder()
                    .implementation(ClassOrInterfaceTypeExpr("java.lang.Void"))
                    .build()
            )
        }

        return contentAnnotationBuilder.build()
    }

    private fun createExampleObject(
        name: String,
        openApiExample: Example
    ): Expr {
        return ExampleObjectAnnotationBuilder()
            .name(name)
            .summary(openApiExample.summary)
            .value(escapeJson(openApiExample.value.toString()))
            .build()
    }

    private fun escapeJson(s: String): String {
        val sb = StringBuilder()
        var pc: Char = 0.toChar()

        for (c in s.toCharArray()) {
            if (c == '"' && pc != '\\') {
                sb.append('\\')
            }
            sb.append(c)
            pc = c
        }

        return sb.toString()
    }

    private fun getElementType(schemaType: TypeExpr): TypeExpr? {
        return if (schemaType is ArrayExpr) {
            schemaType.componentType
        } else if (schemaType is ArrayTypeExpr) {
            schemaType.componentType
        } else {
            if (typeUtils.isCollectionType(schemaType)
            ) (schemaType as ClassOrInterfaceTypeExpr).typeArguments.first()
            else null
        }
    }

    private fun createArrayAnnotation(
        openAPI: OpenAPI,
        schema: Schema<*>,
        elementType: TypeExpr?
    ): Annot {
        return ArraySchemaAnnotationBuilder()
            .schema(
                createSchemaAnnotation(schema)
                    .implementation(typeUtils.resolveImplementationType(openAPI, elementType))
                    .requiredMode(false)
                    .build()
            )
            .build()
    }

    private fun createSchemaAnnotation(schema: Schema<*>?): SchemaAnnotationBuilder {
        val builder = SchemaAnnotationBuilder()

        if (schema != null) {
            val requiredProperties = nonNull(schema.required).stream()
                .map { value: String? -> SimpleLiteralExpr(value) }
                .toList()

            builder.description(schema.description)
                .format(schema.format)
                .nullable(schema.nullable)
                .accessMode(schema.readOnly, schema.writeOnly)
                .requiredProperties(requiredProperties)
        }

        return builder
    }

    private fun createSecurityRequirementsAnnotation(securityRequirements: List<SecurityRequirement>): Annot? {
        val annotations = ArrayList<Expr>()

        for (securityRequirement in securityRequirements) {
            for ((name, values) in securityRequirement) {
                annotations.add(
                    SecurityRequirementAnnotationBuilder()
                        .name(name)
                        .scopes(values)
                        .build()
                )
            }
        }

        return if (annotations.isNotEmpty()
        ) SecurityRequirementsBuilder().value(annotations).build()
        else null
    }

    private fun createApiResponsesAnnotation(
        openAPI: OpenAPI,
        operation: Operation
    ): Annot {
        val responses = if (operation.responses != null) {
            operation.responses.entries.stream()
                .map { response: Map.Entry<String, ApiResponse> -> createApiResponse(openAPI, response) }
                .toList()
        } else {
            ArrayList()
        }

        return ApiResponsesAnnotationBuilder()
            .value(responses)
            .build()
    }

    private fun createMappingAnnotation(
        httpMethod: HttpMethod?,
        path: String,
        operation: Operation
    ): Annot {
        val requestMappingAnnotationBuilder = when (httpMethod) {
            HttpMethod.POST -> RequestMappingAnnotationBuilder.post()
            HttpMethod.GET -> RequestMappingAnnotationBuilder.get()
            HttpMethod.PUT -> RequestMappingAnnotationBuilder.put()
            HttpMethod.PATCH -> RequestMappingAnnotationBuilder.patch()
            HttpMethod.DELETE -> RequestMappingAnnotationBuilder.delete()
            else -> throw UnsupportedOperationException()
        }

        val produces = nonNull(operation.responses).values.stream()
            .flatMap { it: ApiResponse -> nonNull(it.content).keys.stream() }
            .toList()

        val requestBody = operation.requestBody

        requestMappingAnnotationBuilder.value(path)

        if (requestBody != null && requestBody.content != null && !requestBody.content.isEmpty()) {
            val consumes = CollectionBuilder<String>()
                .addAll(requestBody.content.keys)
                .addAll(requestBody.content.keys.stream()
                    .filter { it: String -> it.startsWith("image/") }
                    .map { contentMediaType: String -> "$contentMediaType;charset=UTF-8" }
                    .toList())
                .buildList()
            requestMappingAnnotationBuilder.consumes(consumes)
        }

        return requestMappingAnnotationBuilder.produces(produces)
            .build()
    }

    private fun createApiResponse(
        openAPI: OpenAPI,
        entry: Map.Entry<String, ApiResponse>
    ): Annot {
        val response = entry.value
        val description = response.description

        val contentList: MutableList<Expr?> = if (response.content != null
        ) ArrayList(response.content.entries.stream()
            .map { contentMediaType: Map.Entry<String?, MediaType?> ->
                createContentAnnotation(
                    openAPI,
                    null,
                    fromValue(contentMediaType.key!!),
                    contentMediaType.value
                )
            }
            .toList())
        else ArrayList()

        if (contentList.isEmpty()) {
            contentList.add(createContentAnnotation(openAPI, null, null, null))
        }

        return ApiResponseAnnotationBuilder()
            .responseCode(entry.key)
            .description(description)
            .headers(headers(openAPI, response.headers))
            .content(contentList)
            .build()
    }

    private fun headers(
        openAPI: OpenAPI,
        headersMap: Map<String, Header>?
    ): ArrayExpr? {
        if (headersMap == null) {
            return null
        }

        val headers = headersMap.entries.stream()
            .map { entry: Map.Entry<String?, Header> ->
                val header = entry.value
                val description = header.description
                val required = header.required
                val deprecated = header.deprecated
                val headerSchema = header.schema

                val headerType = if (headerSchema != null
                ) typeUtils.createType(
                    openAPI,
                    headerSchema,
                    null,
                    modelPackageName,
                    null,
                    required
                )
                else null
                HeaderAnnotationBuilder()
                    .name(entry.key)
                    .description(description)
                    .required(required)
                    .deprecated(deprecated)
                    .schema(
                        SchemaAnnotationBuilder()
                            .implementation(headerType)
                            .build()
                    )
                    .build() as Expr
            }.toList()

        return ArrayExpr().values(headers)
    }
}
