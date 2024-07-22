package io.github.potjerodekool.openapi.common.generate

import io.github.potjerodekool.codegen.model.type.TypeKind
import io.github.potjerodekool.codegen.template.model.type.*
import io.github.potjerodekool.openapi.common.SchemaType
import io.github.potjerodekool.openapi.common.SchemaTypeResolver
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.*

open class StandardOpenApiTypeUtils : OpenApiTypeUtils {

    override fun createType(
        openAPI: OpenAPI,
        schema: Schema<*>?,
        extensions: MutableMap<String, Any?>?,
        packageName: String,
        mediaType: ContentType?,
        isRequired: Boolean?
    ): TypeExpr {
        if (schema == null) {
            return ClassOrInterfaceTypeExpr("java.lang.Object")
        }

        val schemaType = SchemaTypeResolver.resolveSchemaType(schema)

        return when (schemaType) {
            SchemaType.ARRAY -> createArrayType(openAPI, schema, packageName, mediaType)
            SchemaType.BINARY -> createMultipartTypeExpression(openAPI)
            SchemaType.BOOLEAN -> createBooleanType(schema, isRequired)
            SchemaType.DATE -> createDateType()
            SchemaType.DATE_TIME -> createDateTimeType()
            SchemaType.EMAIL -> createStringType(null)
            SchemaType.INT32, SchemaType.INT64 -> createIntegerType(schema, isRequired)
            SchemaType.MAP -> createMapType(openAPI, schema, packageName, mediaType, isRequired)
            SchemaType.FLOAT, SchemaType.DOUBLE -> createNumberType(schema, isRequired)
            SchemaType.OBJECT -> {
                val typeArg = ExtensionsHelper.getExtension(
                    schema.extensions,
                    Extensions.TYPE_ARG,
                    String::class.java
                )

                val type = if (typeArg == null) {
                    ClassOrInterfaceTypeExpr("java.lang.Object")
                } else {
                    ClassOrInterfaceTypeExpr(typeArg)
                }

                processExtensions(openAPI, type, packageName, extensions)
                type
            }
            SchemaType.PASSWORD -> createStringType(null)
            SchemaType.STRING -> createStringType(schema)
            SchemaType.UUID -> createUuidType()
            else -> {
                when (schema) {
                    is ByteArraySchema -> throw UnsupportedOperationException()
                    is FileSchema -> throw UnsupportedOperationException()
                    else -> createTypeDefault(
                        openAPI,
                        schema,
                        extensions,
                        packageName,
                        mediaType,
                        isRequired
                    )
                }
            }
        }
    }

    override fun asNonNull(typeExpr: TypeExpr?): TypeExpr {
        if (typeExpr is PrimitiveTypeExpr) {
            return typeExpr
        } else if (typeExpr is ClassOrInterfaceTypeExpr) {
            val name = typeExpr.name
            return when (name) {
                "java.lang.Boolean" -> ClassOrInterfaceTypeExpr("java.lang.Boolean")
                "java.lang.Integer" -> ClassOrInterfaceTypeExpr("java.lang.Integer")
                "java.lang.Long" -> ClassOrInterfaceTypeExpr("java.lang.Long")
                "java.lang.Float" -> ClassOrInterfaceTypeExpr("java.lang.Float")
                "java.lang.Double" -> ClassOrInterfaceTypeExpr("java.lang.Double")
                else -> typeExpr
            }
        }

        throw UnsupportedOperationException()
    }

    private fun asNullable(typeExpr: TypeExpr): TypeExpr {
        return when (typeExpr.typeKind) {
            TypeKind.BOOLEAN -> ClassOrInterfaceTypeExpr("java.lang.Boolean")
            TypeKind.INT -> ClassOrInterfaceTypeExpr("java.lang.Integer")
            TypeKind.LONG -> ClassOrInterfaceTypeExpr("java.lang.Long")
            TypeKind.FLOAT -> ClassOrInterfaceTypeExpr("java.lang.Float")
            TypeKind.DOUBLE -> ClassOrInterfaceTypeExpr("java.lang.Double")
            TypeKind.BYTE -> ClassOrInterfaceTypeExpr("java.lang.Byte")
            TypeKind.SHORT -> ClassOrInterfaceTypeExpr("java.lang.Short")
            TypeKind.CHAR -> ClassOrInterfaceTypeExpr("java.lang.Character")
            else -> typeExpr
        }
    }

    override fun createMultipartTypeExpression(api: OpenAPI): TypeExpr {
        throw UnsupportedOperationException()
    }

    override fun resolveImplementationType(openAPI: OpenAPI, type: TypeExpr?): TypeExpr? {
        return if (type is WildCardTypeExpr) {
            type.expr as TypeExpr
        } else {
            type
        }
    }

    override fun createNumberType(numberSchema: Schema<*>, isRequired: Boolean?): TypeExpr {
        val isNullable = true == numberSchema.nullable

        return if ("double" == numberSchema.format) {
            if (isNullable || false == isRequired) ClassOrInterfaceTypeExpr("java.lang.Double")
            else PrimitiveTypeExpr(TypeKind.DOUBLE)
        } else if ("float" == numberSchema.format) {
            if (isNullable || false == isRequired) ClassOrInterfaceTypeExpr("java.lang.Float")
            else PrimitiveTypeExpr(TypeKind.FLOAT)
        } else {
            ClassOrInterfaceTypeExpr("java.math.BigDecimal")
        }
    }

    override fun createStringType(schema: Schema<*>?): TypeExpr {
        return if (schema?.enum != null) {
            ClassOrInterfaceTypeExpr().packageName("java.lang").simpleName("Enum")
        } else {
            ClassOrInterfaceTypeExpr().packageName("java.lang").simpleName("String")
        }
    }

    override fun createDateType(): TypeExpr {
        return ClassOrInterfaceTypeExpr("java.time.LocalDate")
    }

    override fun createDateTimeType(): TypeExpr {
        return ClassOrInterfaceTypeExpr("java.time.OffsetDateTime")
    }

    override fun createBooleanType(booleanSchema: Schema<*>, isRequired: Boolean?): TypeExpr {
        val isNullable = true == booleanSchema.nullable
        return if (isNullable || false == isRequired) ClassOrInterfaceTypeExpr("java.lang.Boolean")
        else PrimitiveTypeExpr(TypeKind.BOOLEAN)
    }

    override fun createUuidType(): TypeExpr {
        return ClassOrInterfaceTypeExpr("java.util.UUID")
    }

    override fun createMapType(
        openAPI: OpenAPI,
        mapSchema: Schema<*>,
        packageName: String,
        mediaType: ContentType?,
        isRequired: Boolean?
    ): ClassOrInterfaceTypeExpr {
        val keyType = ClassOrInterfaceTypeExpr("java.lang.String")
        val valueType = asNullable(
            createType(
                openAPI,
                mapSchema.additionalProperties as Schema<*>,
                mutableMapOf(),
                packageName,
                mediaType,
                isRequired
            )
        )

        return ClassOrInterfaceTypeExpr("java.util.Map")
            .typeArguments(
                keyType,
                valueType
            )
    }

    override fun createArrayType(
        openAPI: OpenAPI,
        arraySchema: Schema<*>,
        packageName: String,
        mediaType: ContentType?
    ): TypeExpr {
        val componentType = createType(
            openAPI,
            arraySchema.items,
            mutableMapOf(),
            packageName,
            mediaType,
            false
        )

        if (componentType is PrimitiveTypeExpr) {
            return createArrayType(componentType)
        } else {
            val className =
                if (true == arraySchema.items.uniqueItems) "java.util.Set"
                else "java.util.List"

            return ClassOrInterfaceTypeExpr(className)
                .typeArgument(componentType)
        }
    }

    private fun createArrayType(primitiveType: PrimitiveTypeExpr): ArrayTypeExpr {
        return ArrayTypeExpr(primitiveType)
    }

    override fun isCollectionType(typeExpr: TypeExpr?): Boolean {
        if (typeExpr is ClassOrInterfaceTypeExpr) {
            return "java.util.Set" == typeExpr.name
                    || "java.util.List" == typeExpr.name
        }
        return false
    }

    private fun createIntegerType(
        integerSchema: Schema<*>,
        isRequired: Boolean?
    ): TypeExpr {
        val isNullable = true == integerSchema.nullable

        return if ("int64" == integerSchema.format) {
            if (isNullable || false == isRequired) ClassOrInterfaceTypeExpr("java.lang.Long")
            else PrimitiveTypeExpr(TypeKind.LONG)
        } else {
            if (isNullable || false == isRequired) ClassOrInterfaceTypeExpr("java.lang.Integer")
            else PrimitiveTypeExpr(TypeKind.INT)
        }
    }

    private fun processExtensions(
        openAPI: OpenAPI, type: ClassOrInterfaceTypeExpr,
        packageName: String,
        extensions: MutableMap<String, Any?>?
    ) {
        val typeArgs = ExtensionsHelper.getExtension(extensions, Extensions.TYPE_ARGS, List::class.java)

        if (typeArgs != null) {
            typeArgs.forEach { typeArg ->
                if (typeArg is String) {
                    val typeArgType = ClassOrInterfaceTypeExpr(typeArg)
                    type.typeArgument(typeArgType)
                } else if (typeArg is Map<*, *>) {
                    val ref = typeArg["\$ref"] as String?

                    if (ref != null) {
                        val resolvedSchema = SchemaResolver.resolve(openAPI, ref)

                        val typeArgType = createType(
                            openAPI,
                            resolvedSchema.schema(),
                            mutableMapOf(),
                            packageName,
                            null,
                            true
                        )

                        if (typeArgType is ClassOrInterfaceTypeExpr) {
                            typeArgType.name(packageName + "." + resolvedSchema.name())
                        }

                        type.typeArgument(typeArgType)
                    }
                }
            }
        }
    }

    private fun createTypeDefault(
        openAPI: OpenAPI,
        schema: Schema<*>,
        extensions: MutableMap<String, Any?>?,
        packageName: String,
        mediaType: ContentType?,
        isRequired: Boolean?
    ): TypeExpr {
        val resolved = SchemaResolver.resolve(openAPI, schema)
        return when (val resolvedSchema = resolved.schema()) {
            null -> throw NullPointerException("Resolved schema is null")
            is ObjectSchema -> createObjectOrComposedType(openAPI, resolved, extensions, packageName)
            is ComposedSchema -> createObjectOrComposedType(openAPI, resolved, extensions, packageName)
            else -> {
                if (resolvedSchema.javaClass == Schema::class.java) {
                    throw UnsupportedOperationException()
                } else {
                    return createType(
                        openAPI,
                        resolved.schema(),
                        extensions,
                        packageName,
                        mediaType,
                        isRequired
                    )
                }
            }
        }
    }

    private fun createObjectOrComposedType(
        openAPI: OpenAPI,
        resolved: ResolvedSchemaResult,
        extensions: MutableMap<String, Any?>?,
        packageName: String
    ): TypeExpr {
        val name = resolved.name()
        if (name != null) {
            val type = ClassOrInterfaceTypeExpr (packageName + "." + resolved.name())
            processExtensions(openAPI, type, packageName, extensions)
            return type
        } else {
            return ClassOrInterfaceTypeExpr ("java.lang.Object")
        }
    }
}