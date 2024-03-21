package io.github.potjerodekool.openapi.common.generate.model.adapter;

import io.github.potjerodekool.codegen.template.model.annotation.Annot;
import io.github.potjerodekool.codegen.template.model.expression.FieldAccessExpr;
import io.github.potjerodekool.codegen.template.model.expression.IdentifierExpr;
import io.github.potjerodekool.openapi.common.generate.Extensions;
import io.github.potjerodekool.openapi.common.generate.ExtensionsHelper;
import io.github.potjerodekool.openapi.common.generate.model.element.ModelProperty;
import io.github.potjerodekool.openapi.common.util.StringUtils;
import io.swagger.v3.oas.models.media.ObjectSchema;

import static java.util.Optional.ofNullable;

public class JaxsonModelAdapter extends AbstractModelAdapter {

    protected void adaptProperty(final ModelProperty modelProperty,
                                 final ObjectSchema schema,
                                 final ModelProperty property) {
        final var propertySchemaOptional = findPropertySchema(schema, modelProperty.getSimpleName());

        propertySchemaOptional.ifPresent(propertySchema -> {
            final var includeOptional = ofNullable(
                    ExtensionsHelper.getExtension(
                            propertySchema.getExtensions(),
                            Extensions.INCLUDE,
                            String.class
                    )
            ).map(StringUtils::toSnakeCase)
                    .map(StringUtils::toUpperCase);

            includeOptional.ifPresent(include -> {


                final var value = new FieldAccessExpr()
                        .target(new IdentifierExpr("com.fasterxml.jackson.annotation.JsonInclude"))
                        .field(new FieldAccessExpr()
                                .target(new IdentifierExpr("Include"))
                                .field(new IdentifierExpr(include)));

                modelProperty.annotation(
                        new Annot().name("com.fasterxml.jackson.annotation.JsonInclude")
                                .attribute("value", value)
                );
            });
        });
    }
}
