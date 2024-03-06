package io.github.potjerodekool.openapi.common.generate;

import io.github.potjerodekool.codegen.template.model.type.TypeExpr;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

import java.util.Map;

public interface OpenApiTypeUtils {

    TypeExpr createType(OpenAPI openAPI,
                        Schema<?> schema,
                        Map<String, Object> extensions,
                        String packageName,
                        String mediaType,
                        Boolean isRequired);

    TypeExpr asNonNull(TypeExpr typeExpr);

    TypeExpr createMultipartTypeExpression(OpenAPI api);

    TypeExpr resolveImplementationType(OpenAPI openAPI, TypeExpr type);
}
