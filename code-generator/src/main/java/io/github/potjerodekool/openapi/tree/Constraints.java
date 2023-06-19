package io.github.potjerodekool.openapi.tree;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Constraints {

    public static final String X_ASSERT = "x-assert";

    private @Nullable Number minimum;
    private @Nullable Boolean exclusiveMinimum;
    private @Nullable Number maximum;
    private @Nullable Boolean exclusiveMaximum;
    private @Nullable Integer minLength;
    private @Nullable Integer maxLength;
    private @Nullable String pattern;
    private @Nullable Integer minItems;
    private @Nullable Integer maxItems;
    private @Nullable Boolean uniqueItems;
    private @Nullable List<Object> enums;
    private @Nullable Object allowedValue;
    private @Nullable Digits digits;

    private final Map<String, Object> extensions = new HashMap<>();

    public Map<String, Object> extensions() {
        return Collections.unmodifiableMap(extensions);
    }

    public Constraints extensions(final Map<String, Object> extensions) {
        this.extensions.putAll(extensions);
        return this;
    }

    public <T> @Nullable T extension(final String name) {
        return (T) this.extensions.get(name);
    }

    public boolean hasExtension(final String name) {
        return this.extensions.containsKey(name);
    }

    public @Nullable Number minimum() {
        return minimum;
    }

    public Constraints minimum(final @Nullable Number minimum) {
        this.minimum = minimum;
        return this;
    }

    public @Nullable Boolean exclusiveMinimum() {
        return exclusiveMinimum;
    }

    public Constraints exclusiveMinimum(final @Nullable Boolean exclusiveMinimum) {
        this.exclusiveMinimum = exclusiveMinimum;
        return this;
    }

    public @Nullable Number maximum() {
        return maximum;
    }

    public Constraints maximum(final @Nullable Number maximum) {
        this.maximum = maximum;
        return this;
    }

    public @Nullable Boolean exclusiveMaximum() {
        return exclusiveMaximum;
    }

    public Constraints exclusiveMaximum(final @Nullable Boolean exclusiveMaximum) {
        this.exclusiveMaximum = exclusiveMaximum;
        return this;
    }

    public @Nullable Integer minLength() {
        return minLength;
    }

    public Constraints minLength(final @Nullable Integer minLength) {
        this.minLength = minLength;
        return this;
    }

    public @Nullable Integer maxLength() {
        return maxLength;
    }

    public Constraints maxLength(final @Nullable Integer maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    public @Nullable String pattern() {
        return pattern;
    }

    public Constraints pattern(final @Nullable String pattern) {
        this.pattern = pattern;
        return this;
    }

    public @Nullable Integer minItems() {
        return minItems;
    }

    public Constraints minItems(final @Nullable Integer minItems) {
        this.minItems = minItems;
        return this;
    }

    public @Nullable Integer maxItems() {
        return maxItems;
    }

    public Constraints maxItems(final @Nullable Integer maxItems) {
        this.maxItems = maxItems;
        return this;
    }

    public @Nullable Boolean uniqueItems() {
        return uniqueItems;
    }

    public Constraints uniqueItems(final @Nullable Boolean uniqueItems) {
        this.uniqueItems = uniqueItems;
        return this;
    }

    public @Nullable List<Object> enums() {
        return enums;
    }

    public Constraints enums(final @Nullable List<Object> enums) {
        this.enums = enums;
        return this;
    }

    public @Nullable Object allowedValue() {
        return allowedValue;
    }

    public Constraints allowedValue(final @Nullable Object allowedValue) {
        this.allowedValue = allowedValue;
        return this;
    }

    public @Nullable Digits digits() {
        return digits;
    }

    public Constraints digits(final @Nullable Digits digits) {
        this.digits = digits;
        return this;
    }
}
