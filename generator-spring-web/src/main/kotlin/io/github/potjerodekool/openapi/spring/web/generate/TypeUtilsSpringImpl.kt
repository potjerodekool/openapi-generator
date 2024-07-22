package io.github.potjerodekool.openapi.spring.web.generate

import io.github.potjerodekool.codegen.template.model.type.ClassOrInterfaceTypeExpr
import io.github.potjerodekool.codegen.template.model.type.TypeExpr
import io.github.potjerodekool.openapi.common.generate.ContentType
import io.github.potjerodekool.openapi.common.generate.OpenApiTypeUtils
import io.github.potjerodekool.openapi.common.generate.StandardOpenApiTypeUtils
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema

class TypeUtilsSpringImpl : StandardOpenApiTypeUtils(), OpenApiTypeUtils {
    override fun createType(
        openAPI: OpenAPI,
        schema: Schema<*>?,
        extensions: MutableMap<String, Any?>?,
        packageName: String,
        mediaType: ContentType?,
        isRequired: Boolean?
    ): TypeExpr {
        return if (schema == null) {
            ClassOrInterfaceTypeExpr("org.springframework.core.io.Resource")
        } else {
            super.createType(openAPI, schema, extensions, packageName, mediaType, isRequired)
        }
    }

    override fun createMultipartTypeExpression(api: OpenAPI): TypeExpr {
        return ClassOrInterfaceTypeExpr("org.springframework.web.multipart.MultipartFile")
    }
}
