package io.github.potjerodekool.openapi.internal.generate.springmvc;

import io.github.potjerodekool.codegen.model.tree.type.BoundKind;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.TypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.WildCardTypeExpression;
import io.github.potjerodekool.openapi.internal.type.AbstractOpenApiTypeUtils;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.BinarySchema;

public class OpenApiTypeUtilsSpringImpl extends AbstractOpenApiTypeUtils {
    public OpenApiTypeUtilsSpringImpl(final OpenApiTypeUtils delegate) {
        super(delegate);
    }

    @Override
    public TypeExpression createBinaryTypeExpression(final BinarySchema schema,
                                                     final OpenAPI openAPI) {
        final var isNullable = Boolean.TRUE.equals(schema.getNullable());
        return new WildCardTypeExpression(
                BoundKind.EXTENDS,
                createClassOrInterfaceTypeExpression("org.springframework.core.io.Resource", isNullable, openAPI)
        );
    }

    @Override
    public ClassOrInterfaceTypeExpression createMultipartTypeExpression(final OpenAPI openAPI) {
        return new ClassOrInterfaceTypeExpression("org.springframework.web.multipart.MultipartFile");
    }
}
