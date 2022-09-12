package io.github.potjerodekool.openapi.internal;

import com.reprezen.jsonoverlay.Reference;
import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.RequestCycleLocation;
import io.github.potjerodekool.openapi.internal.util.GenerateException;
import io.github.potjerodekool.openapi.internal.util.QualifiedName;
import io.github.potjerodekool.openapi.internal.util.Utils;

import java.io.File;

public class DefaultTypeNameResolver implements TypeNameResolver {

    private static final String SCHEMAS = "schemas";
    private static final String SCHEMAS_SLASH = "schemas/";

    private final File schemaDir;

    public DefaultTypeNameResolver(final OpenApiGeneratorConfig config) {
        this.schemaDir = Utils.requireNonNull(config.getSchemasDir());
    }

    @Override
    public void validateRefString(final String refString) {
        if (!refString.contains("#/components/schemas")) {
            if (schemaDir == null) {
                throw new NullPointerException("schemaDir is null");
            }
            final var absoluteSchemaUri = Utils.toUriString(schemaDir);
            if (!refString.startsWith(absoluteSchemaUri)) {
                throw new GenerateException(refString + " doesn't start with " + absoluteSchemaUri);
            }
        }
    }

    @Override
    public QualifiedName createTypeName(final Reference creatingRef, final SchemaContext schemaContext) {
        final var refString = creatingRef.getNormalizedRef();
        final var absoluteSchemaUri = Utils.toUriString(schemaDir);

        if (!refString.startsWith(absoluteSchemaUri)) {
            throw new GenerateException(refString + " doesn't start with " + absoluteSchemaUri);
        }

        final var path = refString.substring(absoluteSchemaUri.length());

        final var qualifiedName = refString.substring(absoluteSchemaUri.length());
        final var packageSepIndex = qualifiedName.lastIndexOf('/');

        final var nameBuilder = new StringBuilder();
        final String packageName;

        if (packageSepIndex > 0) {
            packageName = removeSchemasPart(path.substring(0, packageSepIndex)).replace('/', '.');
            final var nameSepIndex = path.lastIndexOf('.');
            final var name = path.substring(packageSepIndex + 1, nameSepIndex);
            nameBuilder.append(name);
        } else {
            packageName = "";
            final var nameSepIndex = path.lastIndexOf('.');
            final var name = path.substring(0, nameSepIndex);
            nameBuilder.append(name);
        }

        if (HttpMethod.PATCH == schemaContext.httpMethod()
                && schemaContext.requestCycleLocation() == RequestCycleLocation.REQUEST
                && !nameBuilder.toString().endsWith("Patch")) {
            nameBuilder.append("Patch");
        }

        if (schemaContext.requestCycleLocation() == RequestCycleLocation.RESPONSE) {
            if (!nameBuilder.toString().endsWith("Response")) {
                nameBuilder.append("Response");
            }
        } else if (!nameBuilder.toString().endsWith("Request")) {
            nameBuilder.append("Request");
        }

        nameBuilder.append("Dto");

        final var simpleName = nameBuilder.toString();
        return new QualifiedName(packageName, simpleName);
    }

    protected String removeSchemasPart(final String value) {
        if (value.startsWith(SCHEMAS)) {
            return value.substring(SCHEMAS.length());
        } else if (value.startsWith(SCHEMAS_SLASH)) {
            return value.substring(SCHEMAS_SLASH.length());
        } else {
            return value;
        }
    }
}
