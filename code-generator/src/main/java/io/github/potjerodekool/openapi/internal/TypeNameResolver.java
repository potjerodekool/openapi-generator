package io.github.potjerodekool.openapi.internal;

import com.reprezen.jsonoverlay.Reference;
import io.github.potjerodekool.openapi.internal.util.QualifiedName;

public interface TypeNameResolver {

    void validateRefString(String refString);

    QualifiedName createTypeName(final Reference creatingRef,
                                 final RequestContext requestContext);
}
