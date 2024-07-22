package io.github.potjerodekool.openapi.common.generate.model

import io.github.potjerodekool.codegen.io.Filer
import io.github.potjerodekool.codegen.io.Location
import io.github.potjerodekool.codegen.template.model.annotation.Annot
import io.github.potjerodekool.openapi.common.ApiConfiguration
import io.github.potjerodekool.openapi.common.OpenApiEnvironment
import io.github.potjerodekool.openapi.common.generate.*
import io.github.potjerodekool.openapi.common.generate.annotation.TypeInfo
import io.github.potjerodekool.openapi.common.generate.model.adapter.ModelAdapter
import io.github.potjerodekool.openapi.common.generate.model.builder.JavaModelBuilder
import io.github.potjerodekool.openapi.common.generate.model.element.Model
import io.github.potjerodekool.openapi.common.util.OpenApiUtils.getMediaType
import io.swagger.models.HttpMethod
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.Operation
import io.swagger.v3.oas.models.media.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.function.Consumer

class ModelsGenerator(
    templates: Templates,
    modelPackageName: String,
    openApiEnvironment: OpenApiEnvironment, typeUtils: OpenApiTypeUtils?
) : OpenApiWalkerListener {
    private val templates: Templates

    private val modelPackageName: String

    private val filer: Filer

    private val processed: MutableSet<String?> = HashSet()

    private val modelBuilder: JavaModelBuilder

    private val adapters: MutableList<ModelAdapter> = ArrayList()

    init {
        val applicationContext = openApiEnvironment.applicationContext

        this.templates = templates
        this.modelPackageName = modelPackageName
        this.filer = openApiEnvironment.environment.filer
        this.modelBuilder = JavaModelBuilder(modelPackageName, typeUtils)
        registerDefaultModelAdapters()
        val adapters = applicationContext.getBeansOfType(
            ModelAdapter::class.java
        )
        this.adapters.addAll(adapters)
    }

    private fun registerDefaultModelAdapters() {
        ServiceLoader.load(ModelAdapter::class.java).forEach(
            Consumer { e: ModelAdapter -> adapters.add(e) })
    }

    fun generateModels(
        openAPI: OpenAPI,
        apiConfiguration: ApiConfiguration?
    ) {
        OpenApiWalker(openAPI!!, apiConfiguration!!).walk(this)
    }

    override fun visitContent(
        openAPI: OpenAPI,
        httpMethod: HttpMethod?,
        path: String?,
        operation: Operation?,
        content: Content?,
        apiConfiguration: ApiConfiguration
    ) {
        val pair = getMediaType(
            content!!
        )

        if (pair != null) {
            val contentType = pair.value

            val typeArgs = ExtensionsHelper.getExtension(
                contentType.extensions,
                Extensions.TYPE_ARGS,
                object : TypeInfo<List<Map<String, Any?>>>() {}
            )

            if (typeArgs != null) {
                typeArgs
                    .map { typeArg -> typeArg["\$ref"] as String? }
                    .filterNotNull()
                    .forEach { ref ->
                        val resolved = SchemaResolver.resolve(openAPI, ref)
                        visitSchema(
                            openAPI,
                            httpMethod,
                            path,
                            operation,
                            resolved.schema,
                            pair.key,
                            resolved.name,
                            apiConfiguration
                        )
                    }
            }
        }
    }

    override fun visitSchema(
        openAPI: OpenAPI,
        httpMethod: HttpMethod?,
        path: String?,
        operation: Operation?,
        schema: Schema<*>?,
        contentType: ContentType?,
        schemaName: String?,
        apiConfiguration: ApiConfiguration
    ) {
        if (schema is ArraySchema) {
            visitSchema(
                openAPI,
                httpMethod,
                path,
                operation,
                schema.getItems(),
                contentType,
                null,
                apiConfiguration
            )
            return
        }

        val resolvedSchemaResult = SchemaResolver.resolve(openAPI, schema)

        if (resolvedSchemaResult.schema == null) {
            return
        }

        var name = (if (resolvedSchemaResult.name != null
        ) resolvedSchemaResult.name
        else schemaName)

        if (httpMethod == HttpMethod.PATCH && name != null && !name.lowercase(Locale.getDefault()).contains("patch")) {
            name = "Patch$name"
        }

        if (processed.contains(name)) {
            return
        }

        processed.add(name)

        val resolvedSchema = resolvedSchemaResult.schema

        if (!shouldProcess(resolvedSchema, schema)) {
            return
        }

        val model = buildModel(
            openAPI,
            httpMethod,
            name,
            resolvedSchema,
            contentType,
            apiConfiguration
        )

        adaptModel(model, resolvedSchema, apiConfiguration)
        writeCode(model)

        processProperties(
            openAPI,
            httpMethod,
            path,
            operation,
            resolvedSchema,
            contentType,
            apiConfiguration
        )
    }

    private fun processProperties(
        openAPI: OpenAPI,
        httpMethod: HttpMethod?,
        path: String?,
        operation: Operation?,
        resolvedSchema: Schema<*>,
        contentType: ContentType?,
        apiConfiguration: ApiConfiguration
    ) {
        val properties = Objects.requireNonNullElse(
            resolvedSchema.properties,
            java.util.Map.of()
        )

        if (resolvedSchema is ObjectSchema
            || resolvedSchema is ComposedSchema
        ) {
            properties.values.forEach(Consumer { propertySchema: Schema<*> ->
                visitSchema(
                    openAPI,
                    httpMethod,
                    path,
                    operation,
                    propertySchema,
                    contentType,
                    null,
                    apiConfiguration
                )
            })
        }
    }

    private fun buildModel(
        openAPI: OpenAPI,
        httpMethod: HttpMethod?,
        name: String?,
        resolvedSchema: Schema<*>,
        contentType: ContentType?,
        apiConfiguration: ApiConfiguration
    ): Model {
        generateSuperClass(
            openAPI,
            httpMethod,
            name,
            resolvedSchema,
            contentType,
            apiConfiguration
        )

        val model = modelBuilder.build(
            openAPI,
            httpMethod,
            modelPackageName,
            name,
            resolvedSchema,
            contentType
        )

        if (model.simpleName == null) {
            throw UnsupportedOperationException()
        }

        val date = DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now())

        model.annotation(
            Annot()
                .name("javax.annotation.processing.Generated")
                .attribute("value", javaClass.name)
                .attribute("date", date)
        )

        processExtensions(
            openAPI,
            httpMethod,
            resolvedSchema,
            contentType,
            apiConfiguration
        )

        return model
    }

    private fun generateSuperClass(
        openAPI: OpenAPI,
        httpMethod: HttpMethod?,
        name: String?,
        resolvedSchema: Schema<*>,
        contentType: ContentType?,
        apiConfiguration: ApiConfiguration
    ) {
        if (resolvedSchema.allOf != null && resolvedSchema.allOf.size == 1) {
            //Generate super class
            val parentSchema = resolvedSchema.allOf.first()
            visitSchema(
                openAPI,
                httpMethod,
                name,
                null,
                parentSchema,
                contentType,
                null,
                apiConfiguration
            )
        }
    }

    private fun processExtensions(
        openAPI: OpenAPI,
        httpMethod: HttpMethod?,
        resolvedSchema: Schema<*>,
        contentType: ContentType?,
        apiConfiguration: ApiConfiguration
    ) {
        val superTypeArgs = ExtensionsHelper.getExtension(
            resolvedSchema.extensions,
            Extensions.SUPER_TYPE_ARGS,
            object : TypeInfo<List<Map<String?, Any?>>>() {}
        )

        if (superTypeArgs != null) {
            superTypeArgs
                .map { typeArg -> typeArg["\$ref"] as String? }
                .filterNotNull()
                .forEach { ref: String ->
                    val resolved = SchemaResolver.resolve(openAPI, ref)
                    if (resolved.schema != null) {
                        visitSchema(
                            openAPI,
                            httpMethod,
                            resolved.name,
                            null,
                            resolved.schema,
                            contentType,
                            resolved.name,
                            apiConfiguration
                        )
                    }
                }
        }
    }

    private fun adaptModel(
        model: Model,
        schema: Schema<*>,
        apiConfiguration: ApiConfiguration
    ) {
        if (schema is ObjectSchema) {
            for (adapter in adapters) {
                adapter.adapt(model, schema, apiConfiguration)
            }
        }
    }

    private fun writeCode(model: Model) {
        val template = templates.getInstanceOf("/model/model")
        template.add("model", model)
        val code = template.render()

        val resource = filer.createResource(
            Location.SOURCE_OUTPUT,
            this.modelPackageName,
            model.simpleName + ".java"
        )

        resource.writeToOutputStream(code.toByteArray())
    }

    private fun shouldProcess(schema: Schema<*>,
                              originalSchema: Schema<*>?): Boolean {
        return (schema is ObjectSchema
                || schema is ComposedSchema
                || originalSchema != null && originalSchema.`$ref` != null)
    }
}
