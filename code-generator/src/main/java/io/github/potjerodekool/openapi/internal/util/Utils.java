package io.github.potjerodekool.openapi.internal.util;

import com.reprezen.jsonoverlay.JsonOverlay;
import com.reprezen.kaizen.oasparser.model3.Parameter;
import com.reprezen.kaizen.oasparser.model3.Schema;
import com.reprezen.kaizen.oasparser.ovl3.SchemaImpl;
import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.util.QualifiedName;
import io.github.potjerodekool.openapi.internal.OpenApiContext;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.io.IOException;

public final class Utils {

    private Utils() {
    }

    public static @Nullable String getCreateRef(final Schema schema) {
        final var schemaImpl = (SchemaImpl) schema;
        final var createRef = schemaImpl._getCreatingRef();
        return createRef != null ? createRef.getRefString() : null;
    }

    public static @Nullable String getReference(final @Nullable OpenApiContext openApiContext) {
        if (openApiContext == null) {
            return null;
        }
        final var item = (JsonOverlay<?>) openApiContext.getItem();
        final var createRef = item._getCreatingRef();

        if (createRef != null) {
            final var value = openApiContext.getValue();
            return String.format("%s%s", createRef.getNormalizedRef(), value);
        } else {
            final var creatingRef = getReference(openApiContext.getParent());

            if (creatingRef != null) {
                final String value;

                if (item instanceof Schema s) {
                    final var name = s.getName();
                    value = name != null ? "/" + name : null;
                } else if (item instanceof Parameter p) {
                    value = "?" + p.getName();
                } else {
                    value = null;
                }

                if (value != null) {
                    return String.format("%s%s", creatingRef, value);
                } else {
                    return creatingRef;
                }
            } else {
                return openApiContext.getValue();
            }
        }
    }

    public static QualifiedName resolveQualified(final String path) {
        final var packageSepIndex = path.lastIndexOf('/');

        if (packageSepIndex < 0) {
            final var nameSep = path.lastIndexOf('.');
            final var name = nameSep > 0 ? path.substring(0, nameSep) : path;
            return new QualifiedName(Name.of(""), Name.of(name));
        } else {
            final var packageName = path.substring(0, packageSepIndex).replace('/', '.');
            final var nameSep = path.lastIndexOf('.');
            final var name = nameSep > 0 ? path.substring(packageSepIndex + 1, nameSep) : path.substring(packageSepIndex + 1);
            return new QualifiedName(Name.of(packageName), Name.of(name));
        }
    }

    public static boolean isNullOrTrue(final @Nullable Boolean value) {
        return value == null || Boolean.TRUE.equals(value);
    }

    public static boolean isFalse(final @Nullable Boolean value) {
        if (value == null) {
            return false;
        }
        return Boolean.FALSE.equals(value);
    }

    public static boolean isTrue(final @Nullable Boolean value) {
        if (value == null) {
            return false;
        }
        return Boolean.TRUE.equals(value);
    }

    public static String toUriString(final File file) {
        try {
            return file.getCanonicalFile().toURI().toString();
        } catch (final IOException e) {
            throw new GenerateException(e);
        }
    }

}
