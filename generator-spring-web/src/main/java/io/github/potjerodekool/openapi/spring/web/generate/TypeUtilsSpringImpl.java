package io.github.potjerodekool.openapi.spring.web.generate;

import io.github.potjerodekool.codegen.template.model.type.ClassOrInterfaceTypeExpr;
import io.github.potjerodekool.codegen.template.model.type.TypeExpr;
import io.github.potjerodekool.openapi.common.generate.OpenApiTypeUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.*;

import java.util.Map;

public class TypeUtilsSpringImpl implements OpenApiTypeUtils {

    private final OpenApiTypeUtils delegate;

    public TypeUtilsSpringImpl(final OpenApiTypeUtils delegate) {
        this.delegate = delegate;
    }

    @Override
    public TypeExpr createType(final OpenAPI openAPI,
                               final Schema<?> schema,
                               final Map<String, Object> extensions,
                               final String packageName,
                               final String mediaType,
                               final Boolean isRequired) {
        if (schema == null) {
            return new ClassOrInterfaceTypeExpr("org.springframework.core.io.Resource");
        } else {
            return delegate.createType(openAPI, schema, extensions, packageName, mediaType, isRequired);
        }
    }

    @Override
    public TypeExpr createNumberType(final NumberSchema numberSchema,
                                     final Boolean isRequired) {
        return delegate.createNumberType(numberSchema, isRequired);
    }

    @Override
    public TypeExpr createStringType(final StringSchema schema) {
        return delegate.createStringType(schema);
    }

    @Override
    public TypeExpr createUuidType() {
        return delegate.createUuidType();
    }

    @Override
    public TypeExpr createDateTimeType() {
        return delegate.createDateTimeType();
    }

    @Override
    public TypeExpr createDateType() {
        return delegate.createDateType();
    }

    @Override
    public TypeExpr createBooleanType(final BooleanSchema booleanSchema, final Boolean isRequired) {
        return delegate.createBooleanType(booleanSchema, isRequired);
    }

    @Override
    public TypeExpr createArrayType(final OpenAPI openAPI,
                                    final ArraySchema arraySchema,
                                    final String packageName,
                                    final String mediaType) {
        return delegate.createArrayType(openAPI, arraySchema, packageName, mediaType);
    }

    @Override
    public ClassOrInterfaceTypeExpr createMapType(final OpenAPI openAPI,
                                                  final MapSchema mapSchema,
                                                  final String packageName,
                                                  final String mediaType,
                                                  final Boolean isRequired) {
        return delegate.createMapType(openAPI, mapSchema, packageName, mediaType, isRequired);
    }

    @Override
    public TypeExpr asNonNull(final TypeExpr typeExpr) {
        return delegate.asNonNull(typeExpr);
    }

    @Override
    public TypeExpr createMultipartTypeExpression(final OpenAPI api) {
        return new ClassOrInterfaceTypeExpr("org.springframework.web.multipart.MultipartFile");
    }

    @Override
    public TypeExpr resolveImplementationType(final OpenAPI openAPI, final TypeExpr type) {
        return delegate.resolveImplementationType(openAPI, type);
    }

}
