package io.github.potjerodekool.openapi.spring.web.generate.api

import io.github.potjerodekool.codegen.Environment
import io.github.potjerodekool.codegen.model.element.ElementKind
import io.github.potjerodekool.codegen.model.element.Modifier
import io.github.potjerodekool.codegen.model.tree.expression.Operator
import io.github.potjerodekool.codegen.template.model.annotation.Annot
import io.github.potjerodekool.codegen.template.model.element.MethodElem
import io.github.potjerodekool.codegen.template.model.element.TypeElem
import io.github.potjerodekool.codegen.template.model.element.VariableElem
import io.github.potjerodekool.codegen.template.model.expression.*
import io.github.potjerodekool.codegen.template.model.statement.BlockStm
import io.github.potjerodekool.codegen.template.model.statement.ReturnStm
import io.github.potjerodekool.codegen.template.model.statement.VariableDeclarationStm
import io.github.potjerodekool.codegen.template.model.type.ClassOrInterfaceTypeExpr
import io.github.potjerodekool.codegen.template.model.type.TypeExpr
import io.github.potjerodekool.codegen.template.model.type.VarTypeExp
import io.github.potjerodekool.openapi.common.ApiConfiguration
import io.github.potjerodekool.openapi.common.GeneratorConfig
import io.github.potjerodekool.openapi.common.StatusCodes
import io.github.potjerodekool.openapi.common.generate.ContentType
import io.github.potjerodekool.openapi.common.generate.OpenApiTypeUtils
import io.github.potjerodekool.openapi.common.util.OpenApiUtils.findOkResponse
import io.github.potjerodekool.openapi.common.util.OpenApiUtils.getMediaType
import io.swagger.models.HttpMethod
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.responses.ApiResponse
import java.util.*
import java.util.function.Consumer

class SpringRestControllerGenerator(
    generatorConfig: GeneratorConfig?,
    apiConfiguration: ApiConfiguration?,
    typeUtils: OpenApiTypeUtils?,
    environment: Environment?
) : AbstractApiGenerator(generatorConfig, apiConfiguration, typeUtils, environment) {
    override fun classNameSuffix(): String {
        return "Controller"
    }

    override fun createClass(simpleName: String?): TypeElem {
        val clazz = super.createClass(simpleName)
        clazz.kind(ElementKind.CLASS)

        clazz.annotation(Annot("org.springframework.web.bind.annotation.RestController"))
        clazz.annotation(Annot("org.springframework.web.bind.annotation.CrossOrigin"))

        val separatorIndex = simpleName!!.lastIndexOf("Controller")
        val apiName = simpleName.substring(0, separatorIndex) + "Api"
        val packageName = basePackageName

        clazz.implement(ClassOrInterfaceTypeExpr("$packageName.$apiName"))

        val serviceName = packageName + "." + simpleName.substring(0, separatorIndex) + "ServiceApi"

        val serviceField = VariableElem()
            .kind(ElementKind.FIELD)
            .modifiers(Modifier.PRIVATE, Modifier.FINAL)
            .type(ClassOrInterfaceTypeExpr(serviceName))
            .simpleName("service")

        clazz.enclosedElement(serviceField)

        val constructor = clazz.createConstructor()
        constructor.modifier(Modifier.PUBLIC)

        constructor.parameter(
            VariableElem()
                .kind(ElementKind.PARAMETER)
                .modifier(Modifier.FINAL)
                .type(ClassOrInterfaceTypeExpr(serviceName))
                .simpleName("service")
        )

        val constructorBody = BlockStm()
            .statement(
                BinaryExpr()
                    .left(
                        FieldAccessExpr()
                            .target(IdentifierExpr("this"))
                            .field(IdentifierExpr("service"))
                    )
                    .operator(Operator.ASSIGN)
                    .right(IdentifierExpr("service"))
            )
        constructor.body(constructorBody)

        return clazz
    }

    override fun postProcessOperation(
        openAPI: OpenAPI,
        httpMethod: HttpMethod?,
        path: String?,
        operation: Operation?,
        method: MethodElem?
    ) {
        method!!.annotation(Annot("java.lang.Override"))
        method.modifier(Modifier.PUBLIC)

        addMethodAnnotations(openAPI, httpMethod, path!!, operation!!, method)

        val okResponseOptional: Optional<MutableMap.MutableEntry<String, ApiResponse>> = findOkResponse(
            operation.responses
        )
        val arguments: MutableList<Expr> = ArrayList()

        if (operation.parameters != null) {
            arguments.addAll(
                operation.parameters.stream()
                    .map { parameter: Parameter -> IdentifierExpr(parameter.name) }
                    .toList())
        }

        if (operation.requestBody != null) {
            val requestBody = operation.requestBody
            val multipartContent = requestBody.content[ContentType.MULTIPART_FORM_DATA.type]

            if (multipartContent == null) {
                arguments.add(IdentifierExpr("body"))
            } else {
                val schema = multipartContent.schema as ObjectSchema
                schema.properties.keys.forEach(Consumer { propertyName: String? ->
                    arguments.add(
                        IdentifierExpr(
                            propertyName
                        )
                    )
                })
            }
        }

        arguments.add(
            NewClassExpr()
                .name("$basePackageName.HttpServletRequestWrapper")
                .arguments(java.util.List.of<Expr>(IdentifierExpr("request")))
        )

        val body = okResponseOptional
            .map { okResponse: Map.Entry<String, ApiResponse> ->
                generateOkResponse(
                    openAPI,
                    httpMethod,
                    operation,
                    okResponse,
                    arguments
                )
            }
            .orElseGet { this.generateNotImplemented() }

        method.body(body)
    }

    private fun generateOkResponse(
        openAPI: OpenAPI, httpMethod: HttpMethod?,
        operation: Operation?,
        okResponse: Map.Entry<String, ApiResponse>,
        arguments: List<Expr>
    ): BlockStm {
        val body = BlockStm()

        val statusCode = okResponse.key
        val response = okResponse.value
        val hasContent = (response.content != null
                && !response.content.isEmpty())
        val requestBody = operation!!.requestBody
        val requestBodyType: TypeExpr?

        if (requestBody != null) {
            val pair = getMediaType(requestBody.content)
            requestBodyType = if (pair != null
            ) typeUtils.createType(
                openAPI,
                pair.value.schema,
                null,
                modelPackageName,
                pair.key,
                requestBody.required
            )
            else null
        } else {
            requestBodyType = null
        }

        val serviceMethodCall = MethodInvocationExpr()
            .target(IdentifierExpr("service"))
            .name(operation.operationId)
            .arguments(arguments)

        val isCreateRequest = httpMethod == HttpMethod.POST && StatusCodes.CREATED == okResponse.key

        val pair =
            if (response.content != null
            ) getMediaType(response.content)
            else null

        //Check if the response has an id property
        val responseWithId = pair != null && hasIdProperty(pair.value.schema)

        if (hasContent || isCreateRequest) {
            val variable = VariableDeclarationStm()
                .modifier(Modifier.FINAL)
                .type(VarTypeExp())
                .identifier("result")
                .initExpression(serviceMethodCall)

            body.statement(variable)
        } else {
            body.statement(serviceMethodCall)
        }

        var methodCall: MethodInvocationExpr?

        //Only call created method if response has a single id property
        if (StatusCodes.CREATED == statusCode && responseWithId) {
            val idExpression = if (requestBodyType != null
            ) MethodInvocationExpr()
                .target(IdentifierExpr("result"))
                .name("getId")
            else SimpleLiteralExpr(null)

            val locationVar = VariableDeclarationStm()
                .modifier(Modifier.FINAL)
                .type(VarTypeExp())
                .identifier("location")
                .initExpression(
                    MethodInvocationExpr()
                        .target(ClassOrInterfaceTypeExpr("$basePackageName.ApiUtils"))
                        .name("createLocation")
                        .arguments(
                            IdentifierExpr("request"),
                            idExpression
                        )

                )

            body.statement(locationVar)

            methodCall = MethodInvocationExpr()
                .target(ClassOrInterfaceTypeExpr("org.springframework.http.ResponseEntity"))
                .name("created")
                .argument(IdentifierExpr("location"))
        } else if (StatusCodes.NO_CONTENT == statusCode) {
            methodCall = MethodInvocationExpr()
                .target(ClassOrInterfaceTypeExpr("org.springframework.http.ResponseEntity"))
                .name("noContent")
        } else {
            methodCall = MethodInvocationExpr()
                .target(ClassOrInterfaceTypeExpr("org.springframework.http.ResponseEntity"))
                .name("status")
                .argument(SimpleLiteralExpr(statusCode.toInt()))
        }

        methodCall = if (!hasContent) {
            MethodInvocationExpr()
                .target(methodCall)
                .name("build")
        } else {
            MethodInvocationExpr()
                .target(methodCall)
                .name("body")
                .argument(IdentifierExpr("result"))
        }

        body.statement(ReturnStm(methodCall))
        return body
    }

    private fun hasIdProperty(schema: Schema<*>?): Boolean {
        if (schema == null) {
            return false
        } else if (schema.allOf != null) {
            if (schema.allOf.stream()
                    .anyMatch { allOfSchema: Schema<*>? -> this.hasIdProperty(allOfSchema) }
            ) {
                return true
            }
        }

        return schema.properties != null && schema.properties.containsKey("id")
    }

    private fun generateNotImplemented(): BlockStm {
        val body = BlockStm()

        body.statement(
            ReturnStm(
                MethodInvocationExpr()
                    .target(
                        MethodInvocationExpr()
                            .target(ClassOrInterfaceTypeExpr("org.springframework.http.ResponseEntity"))
                            .name("status")
                            .argument(
                                FieldAccessExpr()
                                    .target(ClassOrInterfaceTypeExpr("org.springframework.http.HttpStatus"))
                                    .field(IdentifierExpr("NOT_IMPLEMENTED"))
                            )
                    ).name("build")
            )
        )

        return body
    }
}
