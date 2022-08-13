package io.github.potjerodekool.openapi.internal;

import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.RequestCycleLocation;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;
import io.github.potjerodekool.openapi.tree.Package;
import io.github.potjerodekool.openapi.internal.util.GenerateException;
import io.github.potjerodekool.openapi.internal.util.Utils;
import com.reprezen.kaizen.oasparser.model3.Schema;
import com.reprezen.kaizen.oasparser.ovl3.SchemaImpl;
import io.github.potjerodekool.openapi.type.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.github.potjerodekool.openapi.internal.util.Utils.requireNonNull;

/**
 Converts a Schema to a OpenApiType
 */
public class SchemaToTypeConverter {

    private static final String SCHEMAS = "schemas";
    private static final String SCHEMAS_SLASH = "schemas/";

    private final File schemaDir;

    public SchemaToTypeConverter(final OpenApiGeneratorConfig config) {
        this.schemaDir = requireNonNull(config.getSchemasDir());
    }

    private File resolveDirOfSchema(final Schema schema,
                                    final File rootDir) {
        final var createRef = Utils.getCreateRef(schema);
        return createRef != null
                ? requireNonNull(new File(rootDir, createRef).getParentFile())
                : rootDir;
    }

    public OpenApiType build(final Schema schema,
                             final File rootDir,
                             final SchemaContext schemaContext) {
        final File dir = resolveDirOfSchema(schema, rootDir);

        final var properties = createProperties(schema, rootDir, schemaContext);

        final var additionalProperties = schema.getAdditionalPropertiesSchema();
        final var hasAdditionalProperties = additionalProperties.getType() != null;

        final var openApiAdditionalProperties = hasAdditionalProperties
                ? new OpenApiProperty(
                        build(additionalProperties, dir, schemaContext),
                    false,
                    schema.getReadOnly(),
                    schema.getWriteOnly()
                  )
                : null;

        return createType(
                schema,
                properties,
                openApiAdditionalProperties,
                schemaContext,
                dir
        );
    }

    private HashMap<String, OpenApiProperty> createProperties(final Schema schema,
                                                              final File rootDir,
                                                              final SchemaContext schemaContext) {
        final var dir = resolveDirOfSchema(schema, rootDir);
        final var properties = new LinkedHashMap<String, OpenApiProperty>();

        schema.getProperties().forEach((propertyName, propertySchema) -> {
            final var required = schema.getRequiredFields().contains(propertyName);
            final var propertyType = build(propertySchema, dir, schemaContext);

            properties.put(propertyName, new OpenApiProperty(
                    propertyType,
                    required,
                    propertySchema.getReadOnly(),
                    propertySchema.getWriteOnly(),
                    schema.getMinimum(),
                    schema.getExclusiveMinimum(),
                    schema.getMaximum(),
                    schema.getExclusiveMaximum(),
                    schema.getMinLength(),
                    schema.getMaxLength(),
                    schema.getPattern(),
                    schema.getMinItems(),
                    schema.getMaxItems(),
                    schema.getUniqueItems(),
                    schema.getEnums()
            ));
        });

        schema.getAllOfSchemas()
                .forEach(otherSchema -> properties.putAll(createProperties(otherSchema, rootDir, schemaContext)));

        return properties;
    }

    private OpenApiType createType(final Schema schema,
                                   final Map<String, OpenApiProperty> properties,
                                   final @Nullable OpenApiProperty openApiAdditionalProperties,
                                   final SchemaContext schemaContext,
                                   final File dir) {
        final var type = schema.getType();
        final var format = schema.getFormat();
        final var nullable = schema.getNullable();

        if (OpenApiStandardTypeEnum.isStandardType(type)) {
            final var typeEnum = OpenApiStandardTypeEnum.fromType(type);
            return new OpenApiStandardType(
                    typeEnum,
                    format,
                    nullable
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
            default -> throw new IllegalArgumentException(String.format("Unsupported type %s", type));
        };
    }

    public OpenApiType build(final String type,
                             final @Nullable String format,
                             final Boolean nullable) {
        final var typeEnum = OpenApiStandardTypeEnum.fromType(type);
        return new OpenApiStandardType(typeEnum, format, nullable);
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
                packageName = null;
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
