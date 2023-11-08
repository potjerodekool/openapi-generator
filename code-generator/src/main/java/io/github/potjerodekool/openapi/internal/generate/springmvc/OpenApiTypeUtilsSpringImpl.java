package io.github.potjerodekool.openapi.internal.generate.springmvc;

import io.github.potjerodekool.codegen.model.tree.type.BoundKind;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.TypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.WildCardTypeExpression;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.tree.media.OpenApiBinarySchema;
import io.github.potjerodekool.openapi.tree.media.OpenApiSchema;

public class OpenApiTypeUtilsSpringImpl extends OpenApiTypeUtils {

    protected TypeExpression createStringTypeExpression(final OpenApiSchema<?> schema) {
        final var isNullable = Boolean.TRUE.equals(schema.nullable());
        final var format = schema.format();

        if ("binary".equals(format)) {
            return new WildCardTypeExpression(
                    BoundKind.EXTENDS,
                    createClassOrInterfaceTypeExpression("org.springframework.core.io.Resource", isNullable)
            );
        }

        return super.createStringTypeExpression(schema);
    }

    @Override
    protected TypeExpression createBinaryTypeExpression(final OpenApiBinarySchema schema) {
        final var isNullable = Boolean.TRUE.equals(schema.nullable());
        return new WildCardTypeExpression(
                BoundKind.EXTENDS,
                createClassOrInterfaceTypeExpression("org.springframework.core.io.Resource", isNullable)
        );
    }

    @Override
    public ClassOrInterfaceTypeExpression createMultipartTypeExpression() {
        return new ClassOrInterfaceTypeExpression("org.springframework.web.multipart.MultipartFile");
    }
}
