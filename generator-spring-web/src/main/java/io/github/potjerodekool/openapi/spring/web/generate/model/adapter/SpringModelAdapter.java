package io.github.potjerodekool.openapi.spring.web.generate.model.adapter;

import io.github.potjerodekool.codegen.template.model.annotation.Annot;
import io.github.potjerodekool.codegen.template.model.annotation.AnnotTarget;
import io.github.potjerodekool.codegen.template.model.expression.FieldAccessExpr;
import io.github.potjerodekool.codegen.template.model.expression.IdentifierExpr;
import io.github.potjerodekool.codegen.template.model.type.ClassOrInterfaceTypeExpr;
import io.github.potjerodekool.openapi.common.generate.model.adapter.ModelAdapter;
import io.github.potjerodekool.openapi.common.generate.model.element.Model;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;

public class SpringModelAdapter implements ModelAdapter {
    @Override
    public void adapt(final Model model, final ObjectSchema schema) {
        final var properties = schema.getProperties();

        if (properties != null) {
            properties.forEach((propertyName, propertySchema) -> {
                if (propertySchema instanceof DateSchema) {
                    adaptDateProperty(propertyName, model);
                }
            });
        }
    }

    private void adaptDateProperty(final String propertyName, final Model model) {
        final var propertyOptional = model.getProperties().stream().filter(property -> property.getSimpleName().equals(propertyName)).findFirst();

        propertyOptional.ifPresent(property -> {
                    final var expr = new FieldAccessExpr()
                            .target(new ClassOrInterfaceTypeExpr()
                                    .name("org.springframework.format.annotation.DateTimeFormat"))
                            .field(
                                    new FieldAccessExpr()
                                            .target(new ClassOrInterfaceTypeExpr()
                                                    .name("ISO")
                                            )
                                            .field(new IdentifierExpr("DATE"))
                            );

                    property.annotation(new Annot()
                            .name("org.springframework.format.annotation.DateTimeFormat")
                            .attribute("iso",
                                    expr
                            )
                            .target(AnnotTarget.FIELD)
                    );
                }
        );
    }
}
