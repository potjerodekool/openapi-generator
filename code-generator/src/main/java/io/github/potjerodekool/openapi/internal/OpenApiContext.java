package io.github.potjerodekool.openapi.internal;

import com.reprezen.jsonoverlay.JsonOverlay;
import com.reprezen.kaizen.oasparser.model3.Schema;
import org.checkerframework.checker.nullness.qual.Nullable;

public interface OpenApiContext {

    @Nullable OpenApiContext getParent();

    JsonOverlay<?> getItem();

    String getValue();

    OpenApiContext child(Schema schema);
}
