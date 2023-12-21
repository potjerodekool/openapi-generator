package io.github.potjerodekool.openapi;

import io.github.potjerodekool.openapi.tree.media.OpenApiObjectSchema;
import io.github.potjerodekool.openapi.tree.media.OpenApiSchema;
import org.stringtemplate.v4.Interpreter;
import org.stringtemplate.v4.ModelAdaptor;
import org.stringtemplate.v4.ST;
import org.stringtemplate.v4.misc.STNoSuchPropertyException;

public class SchemaModelAdapter implements ModelAdaptor<OpenApiSchema> {
    @Override
    public Object getProperty(final Interpreter interpreter,
                              final ST self, final OpenApiSchema model,
                              final Object property,
                              final String propertyName) throws STNoSuchPropertyException {
        return switch (propertyName) {
            case "pck" -> {
                final OpenApiObjectSchema objectSchema = (OpenApiObjectSchema) model;
                yield objectSchema.pck();
            }
            case "name" -> model.name();
            default -> null;
        };
    }
}
