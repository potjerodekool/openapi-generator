package io.github.potjerodekool.openapi.internal;

import io.github.potjerodekool.openapi.internal.util.QualifiedName;
import io.github.potjerodekool.openapi.tree.Constraints;
import io.github.potjerodekool.openapi.tree.Digits;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;
import io.github.potjerodekool.openapi.tree.Package;
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

    private final TypeNameResolver typeNameResolver;

    public SchemaToTypeConverter(final TypeNameResolver typeNameResolver) {
        this.typeNameResolver = typeNameResolver;
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

            final var constraints = new Constraints();
            constraints.minimum(schema.getMinimum());
            constraints.exclusiveMinimum(schema.getExclusiveMinimum());
            constraints.maximum(schema.getMaximum());
            constraints.exclusiveMaximum(schema.getExclusiveMaximum());
            constraints.minLength(schema.getMinLength());
            constraints.maxLength(schema.getMaxLength());
            constraints.pattern(schema.getPattern());
            constraints.minItems(schema.getMinItems());
            constraints.maxItems(schema.getMinItems());
            constraints.uniqueItems(schema.getUniqueItems());
            constraints.enums(schema.getEnums());

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

            properties.put(propertyName, new OpenApiProperty(
                    propertyType,
                    required,
                    propertySchema.getReadOnly(),
                    propertySchema.getWriteOnly(),
                    constraints
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
            typeNameResolver.validateRefString(refString);
            final QualifiedName qualifiedName = typeNameResolver.createTypeName(creatingRef, schemaContext);
            final String packageName = qualifiedName.packageName();
            final var pck = packageName.isEmpty() ? Package.UNNAMED : new Package(packageName);
            return ot.withPackage(pck).withName(qualifiedName.simpleName());
        } else {
            return type;
        }
    }
}
