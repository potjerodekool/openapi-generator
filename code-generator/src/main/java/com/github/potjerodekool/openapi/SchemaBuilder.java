package com.github.potjerodekool.openapi;

import com.github.potjerodekool.openapi.tree.OpenApiProperty;
import com.github.potjerodekool.openapi.tree.Package;
import com.github.potjerodekool.openapi.type.*;
import com.github.potjerodekool.openapi.util.GenerateException;
import com.github.potjerodekool.openapi.util.Utils;
import com.reprezen.kaizen.oasparser.model3.Schema;
import com.reprezen.kaizen.oasparser.ovl3.SchemaImpl;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.github.potjerodekool.openapi.util.Utils.requireNonNull;

public class SchemaBuilder {

    private static final String SCHEMAS = "schemas";
    private static final String SCHEMAS_SLASH = "schemas/";

    private final File schemaDir;

    public SchemaBuilder(final OpenApiGeneratorConfig config) {
        this.schemaDir = requireNonNull(config.getSchemasDir());
    }

    public OpenApiType build(final Schema schema,
                             final File rootDir,
                             final SchemaContext schemaContext) {

        final var createRef = Utils.getCreateRef(schema);
        final File dir;

        if (createRef != null) {
            dir = requireNonNull(new File(rootDir, createRef).getParentFile());
        } else {
            dir = rootDir;
        }

        final var type = schema.getType();
        final var format = schema.getFormat();

        final var properties = new LinkedHashMap<String, OpenApiProperty>();

        schema.getProperties().forEach((propertyName, propertySchema) -> {
            final var required = schema.getRequiredFields().contains(propertyName);
            final var propertyType = build(propertySchema, dir, schemaContext);
            properties.put(propertyName, new OpenApiProperty(propertyType, required, propertySchema.isNullable(), propertySchema.isReadOnly()));
        });

        final var additionalProperties = schema.getAdditionalPropertiesSchema();
        final var hasAdditionalProperties = additionalProperties.getType() != null;

        final var openApiAdditionalProperties = hasAdditionalProperties
                ? new OpenApiProperty(
                        build(additionalProperties, dir, schemaContext),
                    false,
                    schema.isNullable(),
                    schema.isReadOnly()
                  )
                : null;

        return createType(
                type,
                format,
                schema,
                properties,
                openApiAdditionalProperties,
                schemaContext,
                dir
        );
    }

    private OpenApiType createType(final String type,
                                   final String format,
                                   final Schema schema,
                                   final Map<String, OpenApiProperty> properties,
                                   final @Nullable OpenApiProperty openApiAdditionalProperties,
                                   final SchemaContext schemaContext,
                                   final File dir) {

        if (OpenApiStandardType.isStandardType(type)) {
            return new OpenApiStandardType(
                    type,
                    format
            );
        }

        return switch (type) {
            case "object" -> postProcessType(
                    schema,
                    new OpenApiObjectType("object", properties, openApiAdditionalProperties),
                    schemaContext
            );
            case "array" -> {
                final var items = build(schema.getItemsSchema(), dir, schemaContext);
                yield new OpenApiArrayType(items);
            }
            default -> new OpenApiOtherType(type, format, properties, openApiAdditionalProperties);
        };
    }

    public OpenApiType build(final String type,
                             final @Nullable String format) {
        return new OpenApiStandardType(type, format);
    }

    private OpenApiType postProcessType(final Schema schema,
                                        final OpenApiType type,
                                        final SchemaContext schemaContext) {
        if (type instanceof OpenApiObjectType ot) {
            final var schemaImp = (SchemaImpl) schema;

            final var creatingRef = schemaImp._getCreatingRef();

            if (creatingRef == null) {
                return type;
            }

            final var refString = creatingRef.getNormalizedRef();
            final var absoluteSchemaUri = schemaDir.toURI().toString();

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
                packageName = null;
                final var nameSepIndex = path.lastIndexOf('.');
                final var name = path.substring(0, nameSepIndex);
                nameBuilder.append(name);
            }

            if (HttpMethod.PATCH == schemaContext.httpMethod() && !nameBuilder.toString().endsWith("Patch")) {
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

            final var pck = packageName == null ? Package.UNNAMED : new Package(packageName);

            return ot.withPackage(pck).withName(nameBuilder.toString());
        } else {
            return type;
        }
    }

    private String removeSchemasPart(final String value) {
        if (value.startsWith(SCHEMAS)) {
            return value.substring(SCHEMAS.length());
        } else if (value.startsWith(SCHEMAS_SLASH)) {
            return value.substring(SCHEMAS_SLASH.length());
        } else {
            return value;
        }
    }
}
