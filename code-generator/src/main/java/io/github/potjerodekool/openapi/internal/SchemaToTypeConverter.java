package io.github.potjerodekool.openapi.internal;

import com.reprezen.jsonoverlay.Reference;
import com.reprezen.kaizen.oasparser.model3.Schema;
import com.reprezen.kaizen.oasparser.ovl3.SchemaImpl;
import io.github.potjerodekool.codegen.model.util.StringUtils;
import io.github.potjerodekool.openapi.ApiConfiguration;
import io.github.potjerodekool.openapi.internal.util.GenerateException;
import io.github.potjerodekool.openapi.internal.util.Utils;
import io.github.potjerodekool.openapi.tree.Constraints;
import io.github.potjerodekool.openapi.tree.Digits;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;
import io.github.potjerodekool.openapi.tree.Package;
import io.github.potjerodekool.openapi.type.*;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 Converts a Schema to a OpenApiType
 */
public class SchemaToTypeConverter {

    private static final String SCHEMAS = "schemas";
    private static final String SCHEMAS_SLASH = "schemas/";

    private final @Nullable File schemaDir;
    private final String modelPackageName;

    public SchemaToTypeConverter(final ApiConfiguration apiConfiguration) {
        this.schemaDir = apiConfiguration.schemasDir();
        this.modelPackageName = apiConfiguration.modelPackageName();
    }

    private File resolveDirOfSchema(final Schema schema,
                                    final File rootDir) {
        final var createRef = Utils.getCreateRef(schema);

        if (createRef != null) {
            return new File(rootDir, createRef).getParentFile();
        } else {
            return rootDir;
        }
    }

    public OpenApiType build(final Schema schema,
                             final File rootDir,
                             final RequestContext requestContext,
                             final OpenApiContext openApiContext) {
        final File dir = resolveDirOfSchema(schema, rootDir);

        final var properties = createProperties(schema, rootDir, requestContext, openApiContext);
        final var additionalProperties = schema.getAdditionalPropertiesSchema();
        final var hasAdditionalProperties = additionalProperties.getType() != null;

        final var openApiAdditionalProperties = hasAdditionalProperties
                ? new OpenApiProperty(
                        build(additionalProperties, dir, requestContext, openApiContext.child(additionalProperties)),
                    false,
                    schema.getReadOnly(),
                    schema.getWriteOnly(),
                    schema.getDescription()
                  )
                : null;

        return createType(
                schema,
                properties,
                openApiAdditionalProperties,
                requestContext,
                openApiContext,
                dir
        );
    }

    private HashMap<String, OpenApiProperty> createProperties(final Schema schema,
                                                              final File rootDir,
                                                              final RequestContext requestContext,
                                                              final OpenApiContext openApiContext) {
        final var dir = resolveDirOfSchema(schema, rootDir);
        final var properties = new LinkedHashMap<String, OpenApiProperty>();

        schema.getProperties().forEach((propertyName, propertySchema) -> {
            final var required = schema.getRequiredFields().contains(propertyName);
            final var propertyType = build(propertySchema, dir, requestContext, openApiContext.child(propertySchema));

            final var constraints = new Constraints();
            constraints.minimum(propertySchema.getMinimum());
            constraints.exclusiveMinimum(propertySchema.getExclusiveMinimum());
            constraints.maximum(propertySchema.getMaximum());
            constraints.exclusiveMaximum(propertySchema.getExclusiveMaximum());
            constraints.minLength(propertySchema.getMinLength());
            constraints.maxLength(propertySchema.getMaxLength());
            constraints.pattern(propertySchema.getPattern());
            constraints.minItems(propertySchema.getMinItems());
            constraints.maxItems(propertySchema.getMinItems());
            constraints.uniqueItems(propertySchema.getUniqueItems());
            constraints.enums(propertySchema.getEnums());

            //TODO remove. Should use extensions.
            propertySchema.getExtensions().forEach( (k, value) -> {
                if (k.startsWith("x-")) {
                    final var name = k.substring(2);

                    if ("allowed-value".equals(name)) {
                        constraints.allowedValue(value);
                    } else if ("digits".equals(name)) {
                        if (value instanceof Map<?,?> map) {
                            final var integer = (Integer) map.get("integer");
                            final var fraction = (Integer) map.get("fraction");

                            if (integer != null && fraction != null) {
                                constraints.digits(new Digits(integer, fraction));
                            }
                        }
                    }
                }
            });

            constraints.extensions(propertySchema.getExtensions());

            properties.put(propertyName, new OpenApiProperty(
                    propertyType,
                    required,
                    propertySchema.getReadOnly(),
                    propertySchema.getWriteOnly(),
                    propertySchema.getDescription(),
                    constraints
            ));
        });

        schema.getAllOfSchemas()
                .forEach(otherSchema -> properties.putAll(createProperties(otherSchema, rootDir, requestContext, openApiContext.child(otherSchema))));

        return properties;
    }

    private OpenApiType createType(final Schema schema,
                                   final Map<String, OpenApiProperty> properties,
                                   final @Nullable OpenApiProperty openApiAdditionalProperties,
                                   final RequestContext requestContext,
                                   final OpenApiContext openApiContext,
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
                    new OpenApiObjectType( "object", properties, openApiAdditionalProperties)
            );
            case "array" -> {
                final var items = build(schema.getItemsSchema(), dir, requestContext, openApiContext.child(schema.getItemsSchema()));
                yield new OpenApiArrayType(items, schema.getNullable());
            }
            default -> {
                final var createRef = Utils.getReference(openApiContext);
                throw new IllegalArgumentException(String.format("Unsupported type %s in %s", type, createRef));
            }
        };
    }

    public OpenApiType build(final String type,
                             final @Nullable String format,
                             final Boolean nullable) {
        final var typeEnum = OpenApiStandardTypeEnum.fromType(type);
        return new OpenApiStandardType(typeEnum, format, nullable);
    }

    private OpenApiType postProcessType(final Schema schema,
                                        final OpenApiType type) {
        if (type instanceof OpenApiObjectType ot) {
            final var schemaImp = (SchemaImpl) schema;

            final var creatingRef = schemaImp._getCreatingRef();

            if (creatingRef == null) {
                return type;
            }

            final var refString = creatingRef.getNormalizedRef();
            validateRefString(refString);

            final var resolvePackage = resolvePackage(creatingRef, schema);

            final String name;

            if (refString.contains("#/components/schemas/")) {
                final var nameStart = refString.lastIndexOf("/") + 1;
                name = refString.substring(nameStart);
            } else {
                name = null;
            }
            return ot.withPackage(resolvePackage)
                    .withName(name);
        } else {
            return type;
        }
    }

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

    private Package resolvePackage(final Reference creatingRef, final Schema schema) {
        final var packageNameValue = (String) schema.getExtension("x-package-name");

        final Package pck;

        if (StringUtils.hasLength(packageNameValue)) {
            pck = new Package(packageNameValue);
        } else {
            final var normalizedRef = creatingRef.getNormalizedRef();
            final var resolvedPackageName = normalizedRef.contains("#/components/schemas/")
                ? createTypeNameForInternalModel()
                : createTypeNameForExternalModel(creatingRef);
            pck = resolvedPackageName.isEmpty() ? Package.UNNAMED : new Package(resolvedPackageName);
        }
        return pck;
    }

    private String createTypeNameForInternalModel() {
        return modelPackageName != null
                ? modelPackageName
                : "";
    }

    private String createTypeNameForExternalModel(final Reference creatingRef) {
        if (schemaDir == null) {
            return "";
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

        if (packageSepIndex > 0) {
            packageName = removeSchemasPart(path.substring(0, packageSepIndex)).replace('/', '.');
        } else {
            packageName = "";
        }

        return packageName;
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
