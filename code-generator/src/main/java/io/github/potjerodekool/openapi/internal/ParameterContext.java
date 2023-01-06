package io.github.potjerodekool.openapi.internal;

import com.reprezen.jsonoverlay.JsonOverlay;
import com.reprezen.kaizen.oasparser.model3.Parameter;
import com.reprezen.kaizen.oasparser.model3.Schema;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ParameterContext implements OpenApiContext {

    private final Parameter parameter;
    private final OpenApiContext parent;

    public ParameterContext(final Parameter parameter,
                            final OpenApiContext parent) {
        this.parameter = parameter;
        this.parent = parent;
    }

    @Override
    public @Nullable OpenApiContext getParent() {
        return parent;
    }

    @Override
    public JsonOverlay<?> getItem() {
        return (JsonOverlay<?>) parameter;
    }

    @Override
    public String getValue() {
        return parameter.getName();
    }

    @Override
    public OpenApiContext child(final Schema schema) {
        return new SchemaContext(schema, this);
    }
}
