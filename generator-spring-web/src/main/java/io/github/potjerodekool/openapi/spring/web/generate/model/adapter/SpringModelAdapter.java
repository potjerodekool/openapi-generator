package io.github.potjerodekool.openapi.spring.web.generate.model.adapter;

import io.github.potjerodekool.openapi.common.generate.model.adapter.ModelAdapter;
import io.github.potjerodekool.openapi.common.generate.model.element.Annotation;
import io.github.potjerodekool.openapi.common.generate.model.element.AnnotationTarget;
import io.github.potjerodekool.openapi.common.generate.model.element.Model;
import io.github.potjerodekool.openapi.common.generate.model.expresion.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.openapi.common.generate.model.expresion.FieldAccessExpression;
import io.github.potjerodekool.openapi.common.generate.model.expresion.IdentifierExpression;
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
            final var expr = new FieldAccessExpression()
                    .target(new ClassOrInterfaceTypeExpression()
                            .name("org.springframework.format.annotation.DateTimeFormat"))
                    .field(
                            new FieldAccessExpression()
                                    .target(new ClassOrInterfaceTypeExpression()
                                            .name("ISO")
                                    )
                                    .field(new IdentifierExpression()
                                            .name("DATE")
                                    )
                    );

                    property.annotation(new Annotation()
                            .name("org.springframework.format.annotation.DateTimeFormat")
                            .attribute("iso",
                                    expr
                            )
                            .annotationTarget(AnnotationTarget.FIELD)
                    );
                }
        );
    }
}
