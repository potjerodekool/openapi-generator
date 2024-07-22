package io.github.potjerodekool.openapi.common

import io.swagger.v3.oas.models.media.ArraySchema
import io.swagger.v3.oas.models.media.BinarySchema
import io.swagger.v3.oas.models.media.BooleanSchema
import io.swagger.v3.oas.models.media.DateSchema
import io.swagger.v3.oas.models.media.DateTimeSchema
import io.swagger.v3.oas.models.media.EmailSchema
import io.swagger.v3.oas.models.media.IntegerSchema
import io.swagger.v3.oas.models.media.JsonSchema
import io.swagger.v3.oas.models.media.MapSchema
import io.swagger.v3.oas.models.media.NumberSchema
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.PasswordSchema
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import io.swagger.v3.oas.models.media.UUIDSchema

object SchemaTypeResolver {

    @JvmStatic
    fun resolveSchemaType(schema: Schema<*>): SchemaType {
        return when (schema) {
            is JsonSchema -> when (schema.format) {
                "uuid" -> SchemaType.UUID
                "date" -> SchemaType.DATE
                "datetime" -> SchemaType.DATE_TIME
                "int64" -> SchemaType.INT64
                "float" -> SchemaType.FLOAT
                "double" -> SchemaType.DOUBLE
                else -> {
                    val types = schema.types

                    if (types != null
                        && types.size == 1) {
                            return when (types.first()) {
                                "integer" ->
                                    if (schema.format == "int64") SchemaType.INT64
                                    else SchemaType.INT32
                                "string" -> if (schema.enum != null) SchemaType.ENUM else SchemaType.STRING
                                "boolean" -> SchemaType.BOOLEAN
                                "object" -> SchemaType.OBJECT
                                "array" -> SchemaType.ARRAY
                                else -> TODO()
                            }
                    } else {
                        return SchemaType.STRING
                    }
                }
            }

            is IntegerSchema ->
                if (schema.format == "int64") SchemaType.INT32 else SchemaType.INT64

            is NumberSchema ->
                if (schema.format == "double") SchemaType.DOUBLE else SchemaType.FLOAT

            is StringSchema -> {
                if (schema.format != null) TODO()
                else if (schema.enum != null) SchemaType.ENUM
                else SchemaType.STRING
            }
            is EmailSchema -> SchemaType.EMAIL
            is PasswordSchema -> SchemaType.PASSWORD
            is DateSchema -> SchemaType.DATE
            is DateTimeSchema -> SchemaType.DATE_TIME
            is MapSchema -> SchemaType.MAP
            is ArraySchema -> SchemaType.ARRAY
            is UUIDSchema -> SchemaType.UUID
            is BooleanSchema -> SchemaType.BOOLEAN
            is ObjectSchema -> SchemaType.OBJECT
            is BinarySchema -> SchemaType.BINARY
            else -> SchemaType.UNKNOWN
        }
    }
}
