package io.github.potjerodekool.openapi.common.generate

import io.github.potjerodekool.codegen.model.type.TypeKind
import io.github.potjerodekool.codegen.template.model.type.ArrayTypeExpr
import io.github.potjerodekool.codegen.template.model.type.ClassOrInterfaceTypeExpr
import io.github.potjerodekool.codegen.template.model.type.PrimitiveTypeExpr
import io.github.potjerodekool.codegen.template.model.type.TypeExpr
import io.github.potjerodekool.codegen.template.model.type.WildCardTypeExpr
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BinarySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.ByteArraySchema
import io.swagger.v3.oas.models.media.ComposedSchema
import io.swagger.v3.oas.models.media.DateSchema
import io.swagger.v3.oas.models.media.DateTimeSchema
import io.swagger.v3.oas.models.media.EmailSchema
import io.swagger.v3.oas.models.media.FileSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.JsonSchema
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.PasswordSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.media.UUIDSchema

open class StandardOpenApiTypeUtils : OpenApiTypeUtils {

    override fun createType(
        openAPI: OpenAPI,
        schema: Schema<*>?,
        extensions: MutableMap<String, Any?>,
        packageName: String,
        mediaType: ContentType?,
        isRequired: Boolean?
    ): TypeExpr {
        return when (schema) {
            null -> ClassOrInterfaceTypeExpr("java.lang.Object")
            is ArraySchema -> createArrayType(openAPI, schema, packageName, mediaType)
            is BinarySchema -> createMultipartTypeExpression(openAPI)
            is BooleanSchema -> createBooleanType(schema, isRequired)
            is ByteArraySchema -> throw UnsupportedOperationException()
            is DateSchema -> createDateType()
            is DateTimeSchema -> createDateTimeType()
            is EmailSchema -> createStringType(null)
            is FileSchema -> throw UnsupportedOperationException()
            is IntegerSchema -> createIntegerType(schema, isRequired)
            is JsonSchema -> throw UnsupportedOperationException()
            is MapSchema -> createMapType(openAPI, schema, packageName, mediaType, isRequired)
            is NumberSchema -> createNumberType(schema, isRequired)
            is ObjectSchema -> {
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

            is PasswordSchema -> createStringType(null)
            is StringSchema -> createStringType(schema)
            is UUIDSchema -> createUuidType()
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

    override fun asNonNull(typeExpr: TypeExpr?): TypeExpr {
        if (typeExpr is PrimitiveTypeExpr) {
            return typeExpr
        } else if (typeExpr is ClassOrInterfaceTypeExpr) {
            val name = typeExpr.getName()
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
        return when (typeExpr.getTypeKind()) {
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

    override fun createMultipartTypeExpression(api: OpenAPI?): TypeExpr {
        throw UnsupportedOperationException()
    }

    override fun resolveImplementationType(openAPI: OpenAPI?, type: TypeExpr?): TypeExpr? {
        return if (type is WildCardTypeExpr) {
            type.getExpr() as TypeExpr
        } else {
            type
        }
    }

    override fun createNumberType(numberSchema: NumberSchema, isRequired: Boolean?): TypeExpr {
        val isNullable = true == numberSchema.getNullable()

        return if ("double".equals(numberSchema.getFormat())) {
            if (isNullable || false == isRequired) ClassOrInterfaceTypeExpr("java.lang.Double")
            else PrimitiveTypeExpr(TypeKind.DOUBLE)
        } else if ("float".equals(numberSchema.getFormat())) {
            if (isNullable || false == isRequired) ClassOrInterfaceTypeExpr("java.lang.Float")
            else PrimitiveTypeExpr(TypeKind.FLOAT)
        } else {
            ClassOrInterfaceTypeExpr("java.math.BigDecimal")
        }
    }

    override fun createStringType(schema: StringSchema?): TypeExpr {
        return if (schema != null && schema.getEnum() != null) {
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

    override fun createBooleanType(booleanSchema: BooleanSchema, isRequired: Boolean?): TypeExpr {
        val isNullable = true == booleanSchema.getNullable()
        return if (isNullable || false == isRequired) ClassOrInterfaceTypeExpr("java.lang.Boolean")
        else PrimitiveTypeExpr(TypeKind.BOOLEAN)
    }

    override fun createUuidType(): TypeExpr {
        return ClassOrInterfaceTypeExpr("java.util.UUID")
    }

    override fun createMapType(
        openAPI: OpenAPI,
        mapSchema: MapSchema,
        packageName: String,
        mediaType: ContentType?,
        isRequired: Boolean?
    ): ClassOrInterfaceTypeExpr {
        val keyType = ClassOrInterfaceTypeExpr("java.lang.String")
        val valueType = asNullable(
            createType(
                openAPI,
                mapSchema.getAdditionalProperties() as Schema<*>,
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
        arraySchema: ArraySchema,
        packageName: String,
        mediaType: ContentType?
    ): TypeExpr {
        val componentType = createType(
            openAPI,
            arraySchema.getItems(),
            mutableMapOf(),
            packageName,
            mediaType,
            false
        )

        if (componentType is PrimitiveTypeExpr) {
            return createArrayType(componentType)
        } else {
            val className =
                if (true == arraySchema.getItems().getUniqueItems()) "java.util.Set"
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
            return "java.util.Set" == typeExpr.getName()
                    || "java.util.List" == typeExpr.getName()
        }
        return false
    }

    private fun createIntegerType(
        integerSchema: IntegerSchema,
        isRequired: Boolean?
    ): TypeExpr {
        val isNullable = true == integerSchema.getNullable()

        if ("int64".equals(integerSchema.getFormat())) {
            return if (isNullable || false == isRequired) ClassOrInterfaceTypeExpr("java.lang.Long")
            else PrimitiveTypeExpr(TypeKind.LONG)
        } else {
            return if (isNullable || false == isRequired) ClassOrInterfaceTypeExpr("java.lang.Integer")
            else PrimitiveTypeExpr(TypeKind.INT)
        }
    }

    private fun processExtensions(
        openAPI: OpenAPI, type: ClassOrInterfaceTypeExpr,
        packageName: String,
        extensions: MutableMap<String, Any?>
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
        extensions: MutableMap<String, Any?>,
        packageName: String,
        mediaType: ContentType?,
        isRequired: Boolean?
    ): TypeExpr {
        val resolved = SchemaResolver.resolve(openAPI, schema)
        val resolvedSchema = resolved.schema()

        return when (resolvedSchema) {
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
        extensions: MutableMap<String, Any?>,
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

    override fun createBindingType(openAPI: OpenAPI): ClassOrInterfaceTypeExpr? {
        return null
    }
}