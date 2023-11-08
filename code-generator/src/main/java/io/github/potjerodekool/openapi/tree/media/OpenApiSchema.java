package io.github.potjerodekool.openapi.tree.media;

import io.github.potjerodekool.openapi.internal.OpenApiSchemaBuilder;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.math.BigDecimal;
import java.util.*;

public class OpenApiSchema<T> {

    @Nullable
    private final String name;

    @Nullable
    private final String format;

    @Nullable
    private final Boolean nullable;

    private final List<String> requiredProperties = new ArrayList<>();

    @Nullable
    private final  Boolean readOnly;
    @Nullable
    private final Boolean writeOnly;
    @Nullable
    private final String description;
    private final Map<String, Object> extensions = new HashMap<>();

    @Nullable
    private final BigDecimal minimum;
    @Nullable
    private final Boolean exclusiveMinimum;
    @Nullable
    private final BigDecimal maximum;
    @Nullable
    private final Boolean exclusiveMaximum;
    @Nullable
    private final Integer minLength;
    @Nullable
    private final Integer maxLength;
    @Nullable
    private final String pattern;
    @Nullable
    private final Integer minItems;
    @Nullable
    private final Integer maxItems;
    @Nullable
    private final Boolean uniqueItems;
    @Nullable
    private final List<T> enumeration;
    @Nullable
    private final OpenApiSchema<?> itemSchema;

    private final Map<String, OpenApiSchema<?>> properties = new LinkedHashMap<>();

    private final OpenApiSchema<?> additionalProperties;

    public OpenApiSchema(final OpenApiSchemaBuilder schemaBuilder) {
        this.name = schemaBuilder.name();
        this.format = schemaBuilder.format();
        this.nullable = schemaBuilder.nullable();

        if (schemaBuilder.requiredProperties() != null) {
            this.requiredProperties.addAll(schemaBuilder.requiredProperties());
        }

        this.readOnly = schemaBuilder.readOnly();
        this.writeOnly = schemaBuilder.writeOnly();
        this.description = schemaBuilder.description();

        if (schemaBuilder.extensions() != null) {
            this.extensions.putAll(schemaBuilder.extensions());
        }

        this.minimum = schemaBuilder.minimum();
        this.exclusiveMinimum = schemaBuilder.exclusiveMinimum();
        this.maximum = schemaBuilder.maximum();
        this.exclusiveMaximum = schemaBuilder.exclusiveMaximum();
        this.minLength = schemaBuilder.minLength();
        this.maxLength = schemaBuilder.maxLength();
        this.pattern = schemaBuilder.pattern();
        this.minItems = schemaBuilder.minItems();
        this.maxItems = schemaBuilder.maxItems();
        this.uniqueItems = schemaBuilder.uniqueItems();
        this.enumeration = (List<T>) schemaBuilder.enums();

        this.itemSchema = schemaBuilder.itemsSchema();

        //Use put instread of putAll to preserve order
        schemaBuilder.properties().forEach(this.properties::put);

        this.additionalProperties = schemaBuilder.additionalProperties();
    }

    public String name() {
        return this.name;
    }

    public String format() {
        return format;
    }

    public Boolean nullable() {
        return nullable;
    }

    public List<String> required() {
        return requiredProperties;
    }

    public @Nullable Boolean readOnly() {
        return readOnly;
    }

    public @Nullable Boolean writeOnly() {
        return writeOnly;
    }

    public @Nullable String description() {
        return description;
    }

    public Map<String, Object> extensions() {
        return extensions;
    }

    public BigDecimal minimum() {
        return minimum;
    }

    public Boolean exclusiveMinimum() {
        return exclusiveMinimum;
    }

    public BigDecimal maximum() {
        return maximum;
    }

    public Boolean exclusiveMaximum() {
        return exclusiveMaximum;
    }

    public Integer minLength() {
        return minLength;
    }

    public Integer maxLength() {
        return maxLength;
    }

    public String pattern() {
        return pattern;
    }

    public Integer minItems() {
        return minItems;
    }

    public Integer maxItems() {
        return maxItems;
    }

    public Boolean uniqueItems() {
        return uniqueItems;
    }

    public List<T> enumeration() {
        return enumeration;
    }

    public OpenApiSchema<?> itemschema() {
        return itemSchema;
    }

    public Map<String, OpenApiSchema<?>> properties() {
        return properties;
    }

    public OpenApiSchema<?> additionalProperties() {
        return additionalProperties;
    }

}