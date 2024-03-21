package io.github.potjerodekool.openapi.common.generate;

import io.github.potjerodekool.codegen.template.model.type.ClassOrInterfaceTypeExpr;
import io.github.potjerodekool.codegen.template.model.type.TypeExpr;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.*;

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

    TypeExpr createNumberType(NumberSchema numberSchema,
                              Boolean isRequired);

    TypeExpr createStringType(StringSchema schema);

    TypeExpr createDateType();

    TypeExpr createDateTimeType();

    TypeExpr createBooleanType(BooleanSchema booleanSchema,
                               Boolean isRequired);

    TypeExpr createUuidType();

    ClassOrInterfaceTypeExpr createMapType(OpenAPI openAPI,
                                           MapSchema mapSchema,
                                           String packageName,
                                           String mediaType,
                                           Boolean isRequired);

    TypeExpr createArrayType(OpenAPI openAPI,
                             ArraySchema arraySchema,
                             String packageName,
                             String mediaType);
}
