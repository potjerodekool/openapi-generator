package io.github.potjerodekool.openapi.common.generate

import io.github.potjerodekool.codegen.Environment
import io.github.potjerodekool.codegen.io.Location
import io.github.potjerodekool.codegen.model.element.ElementKind
import io.github.potjerodekool.codegen.model.element.Modifier
import io.github.potjerodekool.codegen.model.util.QualifiedName
import io.github.potjerodekool.codegen.model.util.StringUtils
import io.github.potjerodekool.codegen.template.ImportOrganiser
import io.github.potjerodekool.codegen.template.TemplateBasedGenerator
import io.github.potjerodekool.codegen.template.model.TCompilationUnit
import io.github.potjerodekool.codegen.template.model.annotation.Annot
import io.github.potjerodekool.codegen.template.model.element.TypeElem
import io.github.potjerodekool.codegen.template.model.element.VariableElem
import io.github.potjerodekool.codegen.template.model.expression.SimpleLiteralExpr
import io.github.potjerodekool.codegen.template.model.type.ClassOrInterfaceTypeExpr
import io.github.potjerodekool.codegen.template.model.type.TypeExpr
import io.github.potjerodekool.openapi.common.ApiConfiguration
import io.github.potjerodekool.openapi.common.GeneratorConfig
import io.github.potjerodekool.openapi.common.util.OpenApiUtils.findJsonMediaType
import io.github.potjerodekool.openapi.common.util.OpenApiUtils.isImageOrVideo
import io.github.potjerodekool.openapi.common.util.OpenApiUtils.isMultiPart
import io.github.potjerodekool.openapi.common.util.SchemaWithInfo
import io.swagger.models.HttpMethod
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.parameters.Parameter
import io.swagger.v3.oas.models.parameters.RequestBody
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.function.Consumer
import java.util.stream.Collectors
import kotlin.String

abstract class AbstractGenerator protected constructor(
    generatorConfig: GeneratorConfig,
    private val apiConfiguration: ApiConfiguration,
    val typeUtils: OpenApiTypeUtils, private val environment: Environment
) : OpenApiWalkerListener {
    val basePackageName: String = Objects.requireNonNull(
        apiConfiguration.basePackageName,
        generatorConfig::basePackageName
    )

    val modelPackageName: String = apiConfiguration.modelPackageName()

    private val language = generatorConfig.language

    private val compilationUnitMap: MutableMap<String, TCompilationUnit> = HashMap()

    fun generate(openAPI: OpenAPI) {
        OpenApiWalker(openAPI, this.apiConfiguration).walk(this)

        val generator = TemplateBasedGenerator()
        val filer = environment.filer

        val importOrganiser = ImportOrganiser()

        compilationUnitMap.values.forEach(Consumer { cu: TCompilationUnit ->
            //TODO fix importer importOrganiser.organiseImports(cu);
            val code = generator.doGenerate(cu)
            val clazz = cu.elements.first()

            val resource = filer.createResource(
                Location.SOURCE_OUTPUT,
                cu.packageName, clazz.simpleName + ".java"
            )
            resource.writeToOutputStream(code.toByteArray())
        })
    }

    protected fun findOrCreateClass(
        path: String,
        operation: Operation
    ): TypeElem {
        val className = resolveClassName(path, operation)
        val qualifiedName = QualifiedName.from(className)
        val packageName = qualifiedName.packageName().toString()
        val simpleName = qualifiedName.simpleName().toString()

        val cu = compilationUnitMap.computeIfAbsent(className) { key: String? ->
            createCompilationUnit(
                packageName,
                simpleName
            )
        }

        return cu.elements.stream()
            .findFirst()
            .orElse(null)
    }

    private fun createCompilationUnit(
        packageName: String,
        simpleName: String
    ): TCompilationUnit {
        val cu = TCompilationUnit(language)
        cu.packageName(packageName)

        val clazz = createClass(simpleName)
        cu.element(clazz)
        return cu
    }

    protected open fun createClass(simpleName: String?): TypeElem {
        val date = DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now())

        return TypeElem()
            .modifier(Modifier.PUBLIC)
            .simpleName(simpleName)
            .annotation(
                Annot("javax.annotation.processing.Generated")
                    .attribute(SimpleLiteralExpr(javaClass.name))
                    .attribute("date", SimpleLiteralExpr(date))
            )
    }

    protected fun resolveClassName(
        path: String,
        operation: Operation
    ): String {
        return generateClasName(path, operation)
    }

    protected fun generateClasName(
        path: String,
        operation: Operation
    ): String {
        val className = resolveQualifiedName(path, operation)
        val suffix = classNameSuffix()
        return if (suffix == null) className else className + suffix
    }

    protected abstract fun classNameSuffix(): String?

    private fun resolveQualifiedName(
        path: String,
        operation: Operation
    ): String {
        var simpleName: String?

        val pathElements = path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        val simpleNameBuilder = StringBuilder()

        for (pathElement in pathElements) {
            if (!pathElement.startsWith("{")) {
                simpleNameBuilder.append(StringUtils.firstUpper(pathElement))
            }
        }

        simpleName = simpleNameBuilder.toString()

        simpleName = io.github.potjerodekool.openapi.common.util.StringUtils.toValidClassName(simpleName)

        val apiName: String

        if (operation.tags == null || operation.tags.isEmpty()) {
            apiName = StringUtils.firstUpper(simpleName)
        } else {
            val firstTag = operation.tags.first()
            apiName = Arrays.stream(firstTag.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray())
                .map { value: String? -> StringUtils.firstUpper(value) }
                .collect(Collectors.joining())
        }

        return "$basePackageName.$apiName"
    }

    protected open fun createParameters(
        api: OpenAPI,
        operation: Operation,
        httpMethod: HttpMethod
    ): MutableList<VariableElem> {
        val parameters: MutableList<VariableElem> = ArrayList()

        if (operation.parameters != null) {
            parameters.addAll(
                operation.parameters.stream()
                    .map { parameter: Parameter -> createParameter(api, parameter) }
                    .toList()
            )
        }

        parameters.addAll(createRequestBodyParameter(api, operation, httpMethod))
        return parameters
    }

    private fun createRequestBodyParameter(
        api: OpenAPI?,
        operation: Operation,
        httpMethod: HttpMethod
    ): List<VariableElem> {
        val parameters: MutableList<VariableElem> = ArrayList()
        val requestBody = operation.requestBody

        if (requestBody != null) {
            val bodyMediaType = findJsonMediaType(requestBody.content)

            if (bodyMediaType != null) {
                val resolved = SchemaResolver.resolve(api, bodyMediaType.schema)
                if (httpMethod == HttpMethod.PATCH) {
                    val bodyType = createPatchRequestBodyParameter(
                        api,
                        operation,
                        resolved,
                        bodyMediaType
                    )
                    parameters.add(createBodyParameter(operation, bodyType))
                } else {
                    if (isMultiPart(requestBody.content)) {
                        parameters.addAll(createMultiPartBodyParameters(api, requestBody))
                    } else {
                        val bodyType = createRequestBodyParameterDefault(
                            api,
                            operation,
                            bodyMediaType
                        )
                        parameters.add(createBodyParameter(operation, bodyType))
                    }
                }
            } else if (isMultiPart(requestBody.content)) {
                parameters.addAll(createMultiPartBodyParameters(api, requestBody))
            } else if (isImageOrVideo(requestBody.content)) {
                parameters.add(createImageOrVideoParameter(api))
            } else {
                val bodyType: TypeExpr = ClassOrInterfaceTypeExpr("java.lang.Object")
                parameters.add(createBodyParameter(operation, bodyType))
            }
        }

        return parameters
    }

    private fun createMultiPartBodyParameters(
        api: OpenAPI?,
        requestBody: RequestBody
    ): List<VariableElem> {
        val parameters = ArrayList<VariableElem>()

        val objectSchema = requestBody.content[ContentType.MULTIPART_FORM_DATA.type]!!.schema as ObjectSchema

        val properties = objectSchema.properties

        properties.forEach { (name: String, propertySchema: Schema<*>?) ->
            val type = typeUtils.createType(
                api,
                propertySchema,
                mapOf(),
                modelPackageName,
                null,
                null
            )
            parameters.add(createParameter(type, name))
        }
        return parameters
    }

    private fun createParameter(
        typeExpr: TypeExpr,
        name: String
    ): VariableElem {
        return VariableElem()
            .kind(ElementKind.PARAMETER)
            .modifier(Modifier.FINAL)
            .type(typeExpr)
            .simpleName(name)
    }

    private fun createImageOrVideoParameter(api: OpenAPI?): VariableElem {
        val bodyType = typeUtils.createMultipartTypeExpression(api)
        return createParameter(bodyType, "body")
    }

    protected open fun createBodyParameter(operation: Operation?, bodyType: TypeExpr): VariableElem {
        return createParameter(bodyType, "body")
    }

    private fun createRequestBodyParameterDefault(
        api: OpenAPI?,
        operation: Operation,
        bodyMediaType: SchemaWithInfo
    ): TypeExpr {
        val requestBody = operation.requestBody

        return typeUtils.createType(
            api,
            bodyMediaType.schema,
            null,
            modelPackageName,
            bodyMediaType.mediaType,
            requestBody.required
        )
    }


    private fun createPatchRequestBodyParameter(
        api: OpenAPI?,
        operation: Operation,
        resolved: ResolvedSchemaResult,
        bodyMediaType: SchemaWithInfo
    ): TypeExpr {
        val requestBody = operation.requestBody
        val bodyType: TypeExpr

        var schemaName = resolved.name
        val bodyObjectSchema = resolved.schema

        val type = typeUtils.createType(
            api,
            bodyObjectSchema,
            null,
            modelPackageName,
            bodyMediaType.mediaType,
            requestBody.required
        ) as ClassOrInterfaceTypeExpr
        bodyType = type

        if (bodyObjectSchema !is ArraySchema) {
            var patchType = type

            if (patchType.typeArguments != null && patchType.typeArguments.size == 1) {
                val resolvedItem = SchemaResolver.resolve(api, resolved.schema.items)
                schemaName = resolvedItem.name
                patchType = type.typeArguments.first() as ClassOrInterfaceTypeExpr
            }

            val simpleName = if (!schemaName.lowercase(Locale.getDefault()).contains("patch")
            ) "Patch$schemaName"
            else schemaName
            patchType.name("$modelPackageName.$simpleName")
        }

        return bodyType
    }

    protected open fun createParameter(
        openAPI: OpenAPI,
        openApiParameter: Parameter
    ): VariableElem {
        var type = typeUtils.createType(
            openAPI,
            openApiParameter.schema,
            null,
            modelPackageName,
            null,
            openApiParameter.required
        )

        if (openApiParameter.required == true) {
            type = typeUtils.asNonNull(type)
        }

        return VariableElem()
            .kind(ElementKind.PARAMETER)
            .modifier(Modifier.FINAL)
            .type(type)
            .simpleName(openApiParameter.name)
    }
}
