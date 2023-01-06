package io.github.potjerodekool.openapi.internal;

import com.reprezen.jsonoverlay.JsonOverlay;
import com.reprezen.kaizen.oasparser.model3.Parameter;
import com.reprezen.kaizen.oasparser.model3.Path;
import com.reprezen.kaizen.oasparser.model3.Schema;
import org.checkerframework.checker.nullness.qual.Nullable;

public class PathContext implements OpenApiContext {

    private final Path path;
    private final String pathStr;

    public PathContext(final Path path,
                       final String pathStr) {
        this.path = path;
        this.pathStr = pathStr;
    }

    @Override
    public JsonOverlay<?> getItem() {
        return (JsonOverlay<?>) path;
    }

    @Override
    public String getValue() {
        return pathStr;
    }

    @Override
    public @Nullable OpenApiContext getParent() {
        return null;
    }

    @Override
    public SchemaContext child(final Schema schema) {
        return new SchemaContext(schema, this);
    }

    public ParameterContext child(final Parameter parameter) {
        return new ParameterContext(parameter, this);
    }
}
