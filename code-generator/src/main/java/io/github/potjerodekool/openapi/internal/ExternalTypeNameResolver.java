package io.github.potjerodekool.openapi.internal;

import com.reprezen.jsonoverlay.Reference;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.internal.util.QualifiedName;
import io.github.potjerodekool.openapi.internal.util.Utils;

public class ExternalTypeNameResolver implements TypeNameResolver {

    private static final String SCHEMAS_SLASH = "schemas/";
    private final String modelPackageName;

    public ExternalTypeNameResolver(final OpenApiGeneratorConfig config) {
        this.modelPackageName = Utils.requireNonNull(config.getModelPackageName());
    }

    @Override
    public void validateRefString(final String refString) {
    }

    @Override
    public QualifiedName createTypeName(final Reference creatingRef, final SchemaContext schemaContext) {
        final var refString = creatingRef.getNormalizedRef();
        final int sepIndex = refString.indexOf(SCHEMAS_SLASH) + SCHEMAS_SLASH.length();
        final var simpleName = refString.substring(sepIndex);

        if (modelPackageName == null) {
            throw new NullPointerException("model package name is null");
        }
        return new QualifiedName(modelPackageName, simpleName);
    }

}
