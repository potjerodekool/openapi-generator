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
                        ContentType mediaType,
                        Boolean isRequired);

    TypeExpr asNonNull(TypeExpr typeExpr);

    TypeExpr createMultipartTypeExpression(OpenAPI api);

    TypeExpr resolveImplementationType(OpenAPI openAPI, TypeExpr type);

    TypeExpr createNumberType(Schema<?> numberSchema,
                              Boolean isRequired);

    TypeExpr createStringType(Schema<?> schema);

    TypeExpr createDateType();

    TypeExpr createDateTimeType();

    TypeExpr createBooleanType(Schema<?> booleanSchema,
                               Boolean isRequired);

    TypeExpr createUuidType();

    ClassOrInterfaceTypeExpr createMapType(OpenAPI openAPI,
                                           Schema<?> mapSchema,
                                           String packageName,
                                           ContentType mediaType,
                                           Boolean isRequired);

    TypeExpr createArrayType(OpenAPI openAPI,
                             Schema<?> arraySchema,
                             String packageName,
                             ContentType mediaType);

    boolean isCollectionType(TypeExpr typeExpr);

}
