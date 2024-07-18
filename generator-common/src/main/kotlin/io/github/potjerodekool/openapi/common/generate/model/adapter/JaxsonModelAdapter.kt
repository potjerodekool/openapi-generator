package io.github.potjerodekool.openapi.common.generate.model.adapter

import io.github.potjerodekool.codegen.template.model.annotation.Annot
import io.github.potjerodekool.codegen.template.model.expression.FieldAccessExpr
import io.github.potjerodekool.codegen.template.model.expression.IdentifierExpr
import io.github.potjerodekool.openapi.common.ApiConfiguration
import io.github.potjerodekool.openapi.common.generate.Extensions
import io.github.potjerodekool.openapi.common.generate.ExtensionsHelper
import io.github.potjerodekool.openapi.common.generate.model.element.ModelProperty
import io.github.potjerodekool.openapi.common.util.StringUtils
import io.swagger.v3.oas.models.media.ObjectSchema
import io.swagger.v3.oas.models.media.Schema
import java.util.*

class JaxsonModelAdapter : AbstractModelAdapter() {
    override fun adaptProperty(
        modelProperty: ModelProperty,
        schema: ObjectSchema,
        apiConfiguration: ApiConfiguration
    ) {
        val propertySchemaOptional = findPropertySchema(schema, modelProperty.simpleName)

        propertySchemaOptional.ifPresent { propertySchema: Schema<*> ->
            val includeOptional = Optional.ofNullable<String>(
                ExtensionsHelper.getExtension(
                    propertySchema.extensions,
                    Extensions.INCLUDE,
                    String::class.java
                )
            ).map { obj: String? -> StringUtils.toSnakeCase(obj) }
                .map { obj: String? -> StringUtils.toUpperCase(obj) }
            includeOptional.ifPresent { include: String? ->
                val value = FieldAccessExpr()
                    .target(IdentifierExpr("com.fasterxml.jackson.annotation.JsonInclude"))
                    .field(
                        FieldAccessExpr()
                            .target(IdentifierExpr("Include"))
                            .field(IdentifierExpr(include))
                    )
                modelProperty.annotation(
                    Annot().name("com.fasterxml.jackson.annotation.JsonInclude")
                        .attribute("value", value)
                )
            }
        }
    }
}
