package io.github.potjerodekool.openapi.internal;

import com.reprezen.jsonoverlay.JsonOverlay;
import com.reprezen.kaizen.oasparser.model3.Schema;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SchemaContext implements OpenApiContext {

    private final Schema schema;
    private final OpenApiContext parent;

    SchemaContext(final Schema schema,
                  final OpenApiContext parent) {
        this.schema = schema;
        this.parent = parent;
    }

    @Override
    public OpenApiContext child(final Schema schema) {
        return new SchemaContext(schema, this);
    }

    @Override
    public JsonOverlay<?> getItem() {
        return (JsonOverlay<?>) schema;
    }

    @Override
    public String getValue() {
        return schema.getName();
    }

    @Override
    public @Nullable OpenApiContext getParent() {
        return parent;
    }
}
