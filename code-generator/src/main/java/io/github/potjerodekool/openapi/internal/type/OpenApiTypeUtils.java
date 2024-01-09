package io.github.potjerodekool.openapi.internal.type;

import io.github.potjerodekool.codegen.model.tree.type.*;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.*;

import java.util.List;

public interface OpenApiTypeUtils {

    TypeExpression createTypeExpression(Schema<?> schema,
                                        OpenAPI openAPI);

    TypeExpression createTypeExpression(String mediaType,
                                        Schema<?> schema,
                                        OpenAPI openAPI);

    TypeExpression createObjectTypeExpression(ObjectSchema schema,
                                              OpenAPI openAPI);


    TypeExpression createStringTypeExpression(Schema<?> schema,
                                              OpenAPI openAPI);

    TypeExpression createUUIDTypeExpression(UUIDSchema schema,
                                            OpenAPI openAPI);

    TypeExpression createDateTimeExpression(DateTimeSchema openApiDateTimeSchema,
                                            OpenAPI openAPI);

    TypeExpression createIntegerTypeExpression(IntegerSchema schema,
                                               OpenAPI openAPI);

    TypeExpression createBooleanTypeExpression(BooleanSchema schema,
                                               OpenAPI openAPI);

    TypeExpression createNumberTypeExpression(NumberSchema schema,
                                              OpenAPI openAPI);

    TypeExpression createDateTypeExpression(DateSchema schema,
                                            OpenAPI openAPI);

    TypeExpression createArrayTypeExpression(ArraySchema schema,
                                             OpenAPI openAPI);

    TypeExpression createBinaryTypeExpression(BinarySchema schema,
                                              OpenAPI openAPI);

    ClassOrInterfaceTypeExpression createClassOrInterfaceTypeExpression(String name,
                                                                        boolean isNullable,
                                                                        OpenAPI openAPI);

    ClassOrInterfaceTypeExpression createClassOrInterfaceTypeExpression(String name,
                                                                        Schema<?> schema,
                                                                        OpenAPI openAPI);

    ClassOrInterfaceTypeExpression createClassOrInterfaceTypeExpression(String name,
                                                                        List<TypeExpression> typeArguments,
                                                                        boolean isNullable,
                                                                        OpenAPI openAPI);

    TypeExpression createMapTypeExpression(MapSchema schema,
                                           OpenAPI openAPI);

    public abstract ClassOrInterfaceTypeExpression createMultipartTypeExpression(OpenAPI openAPI);

    TypeExpression resolveImplementationType(TypeExpression type,
                                             OpenAPI openAPI);
}
