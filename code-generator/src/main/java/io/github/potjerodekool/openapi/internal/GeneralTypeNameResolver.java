package io.github.potjerodekool.openapi.internal;

import com.reprezen.jsonoverlay.Reference;
import io.github.potjerodekool.openapi.*;
import io.github.potjerodekool.openapi.internal.util.GenerateException;
import io.github.potjerodekool.openapi.internal.util.QualifiedName;
import io.github.potjerodekool.openapi.internal.util.Utils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;

public class GeneralTypeNameResolver implements TypeNameResolver {

    private static final String SCHEMAS = "schemas";
    private static final String SCHEMAS_SLASH = "schemas/";

    private final @Nullable File schemaDir;
    private final @Nullable String modelPackageName;

    public GeneralTypeNameResolver(final ApiConfiguration apiConfiguration) {
        this.schemaDir = apiConfiguration.schemasDir();
        this.modelPackageName = apiConfiguration.modelPackageName();
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
    public QualifiedName createTypeName(final Reference creatingRef, final RequestContext requestContext) {
        final var normalizedRef = creatingRef.getNormalizedRef();

        if (normalizedRef.contains("#/components/schemas/")) {
            return createTypeNameForInternalModel(creatingRef, requestContext);
        } else {
            return createTypeNameForExternalModel(creatingRef, requestContext);
        }
    }

    private QualifiedName createTypeNameForInternalModel(final Reference creatingRef, final RequestContext requestContext) {
        final var refString = creatingRef.getNormalizedRef();
        final int sepIndex = refString.indexOf(SCHEMAS_SLASH) + SCHEMAS_SLASH.length();
        final var packageName = modelPackageName != null
                ? modelPackageName
                : "";
        final var simpleNameBase = refString.substring(sepIndex);
        final var simpleName = generateSimpleName(requestContext, simpleNameBase);
        return new QualifiedName(packageName, simpleName);
    }

    private QualifiedName createTypeNameForExternalModel(final Reference creatingRef,
                                                         final RequestContext requestContext) {
        if (schemaDir == null) {
            return new QualifiedName("", "");
        }

        final var refString = creatingRef.getNormalizedRef();
        final var absoluteSchemaUri = Utils.toUriString(schemaDir);

        if (!refString.startsWith(absoluteSchemaUri)) {
            throw new GenerateException(refString + " doesn't start with " + absoluteSchemaUri);
        }

        final var path = refString.substring(absoluteSchemaUri.length());

        final var qualifiedName = refString.substring(absoluteSchemaUri.length());
        final var packageSepIndex = qualifiedName.lastIndexOf('/');

        final String packageName;
        final String simpleNameBase;

        if (packageSepIndex > 0) {
            packageName = removeSchemasPart(path.substring(0, packageSepIndex)).replace('/', '.');
            final var nameSepIndex = path.lastIndexOf('.');
            simpleNameBase = path.substring(packageSepIndex + 1, nameSepIndex);
        } else {
            packageName = "";
            final var nameSepIndex = path.lastIndexOf('.');
            simpleNameBase = path.substring(0, nameSepIndex);
        }

        final var simpleName = generateSimpleName(requestContext, simpleNameBase);
        return new QualifiedName(packageName, simpleName);
    }

    private String generateSimpleName(final RequestContext requestContext,
                                      final String simpleNameBase) {
        final var nameBuilder = new StringBuilder(simpleNameBase);

        if (HttpMethod.PATCH == requestContext.httpMethod()
                && requestContext.requestCycleLocation() == RequestCycleLocation.REQUEST
                && !endsWith(nameBuilder.toString(), "Patch", "Dto")) {
            nameBuilder.append("Patch");
        }

        if (requestContext.requestCycleLocation() == RequestCycleLocation.RESPONSE) {
            if (!endsWith(nameBuilder.toString(), "Response", "Dto")) {
                nameBuilder.append("Response");
            }
        } else if (!endsWith(nameBuilder.toString(), "Request", "Dto")) {
            nameBuilder.append("Request");
        }

        if (!nameBuilder.toString().endsWith("Dto")) {
            nameBuilder.append("Dto");
        }

        return nameBuilder.toString();
    }

    private boolean endsWith(final String value,
                             final String option1,
                             final String option2) {
        return value.endsWith(option1)
                || value.endsWith(option2);
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
