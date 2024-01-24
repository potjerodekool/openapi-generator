package io.github.potjerodekool.openapi.internal.generate.incurbation;

import io.github.potjerodekool.codegen.template.model.type.TypeExpr;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

public interface TypeUtils {
    TypeExpr createType(OpenAPI openAPI,
                        Schema<?> schema,
                        String packageName,
                        String mediaType);

    TypeExpr asNonNull(TypeExpr typeExpr);

    TypeExpr createMultipartTypeExpression(OpenAPI api);
}
