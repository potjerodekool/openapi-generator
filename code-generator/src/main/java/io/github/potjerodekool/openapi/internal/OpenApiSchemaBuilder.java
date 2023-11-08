package io.github.potjerodekool.openapi.internal;

import io.github.potjerodekool.openapi.tree.media.OpenApiSchema;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class OpenApiSchemaBuilder {

    private String name;
    private String format;
    private Boolean nullable;
    private List<String> requiredProperties;
    private Boolean readOnly;
    private Boolean writeOnly;
    private String description;
    private Map<String, Object> extensions;
    private OpenApiSchema<?> itemsSchema;
    private final Map<String, OpenApiSchema<?>> properties = new LinkedHashMap<>();
    private OpenApiSchema<?> additionalProperties;

    private @Nullable BigDecimal minimum;
    private @Nullable Boolean exclusiveMinimum;
    private @Nullable BigDecimal maximum;
    private @Nullable Boolean exclusiveMaximum;
    private @Nullable Integer minLength;
    private @Nullable Integer maxLength;
    private @Nullable String pattern;
    private @Nullable Integer minItems;
    private @Nullable Integer maxItems;
    private @Nullable Boolean uniqueItems;
    private @Nullable List<?> enums;

    public String format() {
        return format;
    }

    public OpenApiSchemaBuilder format(final String format) {
        this.format = format;
        return this;
    }

    public String name() {
        return this.name;
    }

    public OpenApiSchemaBuilder name(final String name) {
        this.name = name;
        return this;
    }

    public Boolean nullable() {
        return nullable;
    }

    public OpenApiSchemaBuilder nullable(final Boolean nullable) {
        this.nullable = nullable;
        return this;
    }

    public List<String> requiredProperties() {
        return requiredProperties;
    }

    public OpenApiSchemaBuilder requiredProperties(final List<String> requiredProperties) {
        this.requiredProperties = requiredProperties;
        return this;
    }

    public Boolean readOnly() {
        return readOnly;
    }

    public OpenApiSchemaBuilder readOnly(final Boolean readOnly) {
        this.readOnly = readOnly;
        return this;
    }

    public Boolean writeOnly() {
        return writeOnly;
    }

    public OpenApiSchemaBuilder writeOnly(final Boolean writeOnly) {
        this.writeOnly = writeOnly;
        return this;
    }

    public String description() {
        return description;
    }

    public OpenApiSchemaBuilder description(final String description) {
        this.description = description;
        return this;
    }

    public Map<String, Object> extensions() {
        return extensions;
    }

    public OpenApiSchemaBuilder extensions(final Map<String, Object> extensions) {
        this.extensions = extensions;
        return this;
    }

    public OpenApiSchema<?> itemsSchema() {
        return itemsSchema;
    }

    public OpenApiSchemaBuilder itemsSchema(final OpenApiSchema<?> itemsSchema) {
        this.itemsSchema = itemsSchema;
        return this;
    }

    public Map<String, OpenApiSchema<?>> properties() {
        return properties;
    }

    public OpenApiSchemaBuilder properties(final Map<String, OpenApiSchema<?>> properties) {
        //Use put instead of putAll to preserve order
        properties.forEach(this.properties::put);
        return this;
    }

    public OpenApiSchema<?> additionalProperties() {
        return additionalProperties;
    }

    public OpenApiSchemaBuilder additionalProperties(final OpenApiSchema<?> additionalProperties) {
        this.additionalProperties = additionalProperties;
        return this;
    }

    public @Nullable BigDecimal minimum() {
        return minimum;
    }

    public OpenApiSchemaBuilder minimum(final @Nullable BigDecimal minimum) {
        this.minimum = minimum;
        return this;
    }

    public @Nullable Boolean exclusiveMinimum() {
        return exclusiveMinimum;
    }

    public OpenApiSchemaBuilder exclusiveMinimum(final @Nullable Boolean exclusiveMinimum) {
        this.exclusiveMinimum = exclusiveMinimum;
        return this;
    }

    public @Nullable BigDecimal maximum() {
        return maximum;
    }

    public OpenApiSchemaBuilder maximum(final @Nullable BigDecimal maximum) {
        this.maximum = maximum;
        return this;
    }

    public @Nullable Boolean exclusiveMaximum() {
        return exclusiveMaximum;
    }

    public OpenApiSchemaBuilder exclusiveMaximum(final @Nullable Boolean exclusiveMaximum) {
        this.exclusiveMaximum = exclusiveMaximum;
        return this;
    }

    public @Nullable Integer minLength() {
        return minLength;
    }

    public OpenApiSchemaBuilder minLength(final @Nullable Integer minLength) {
        this.minLength = minLength;
        return this;
    }

    public @Nullable Integer maxLength() {
        return maxLength;
    }

    public OpenApiSchemaBuilder maxLength(final @Nullable Integer maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    public @Nullable String pattern() {
        return pattern;
    }

    public OpenApiSchemaBuilder pattern(final @Nullable String pattern) {
        this.pattern = pattern;
        return this;
    }

    public @Nullable Integer minItems() {
        return minItems;
    }

    public OpenApiSchemaBuilder minItems(final @Nullable Integer minItems) {
        this.minItems = minItems;
        return this;
    }

    public @Nullable Integer maxItems() {
        return maxItems;
    }

    public OpenApiSchemaBuilder maxItems(final @Nullable Integer maxItems) {
        this.maxItems = maxItems;
        return this;
    }

    public @Nullable Boolean uniqueItems() {
        return uniqueItems;
    }

    public OpenApiSchemaBuilder uniqueItems(final @Nullable Boolean uniqueItems) {
        this.uniqueItems = uniqueItems;
        return this;
    }

    public @Nullable List<?> enums() {
        return enums;
    }

    public OpenApiSchemaBuilder enums(final @Nullable List<?> enums) {
        this.enums = enums;
        return this;
    }
}

