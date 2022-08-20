package io.github.potjerodekool.openapi.tree;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public class Constraints {

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

    public @Nullable Number minimum() {
        return minimum;
    }

    public void minimum(final @Nullable Number minimum) {
        this.minimum = minimum;
    }

    public @Nullable Boolean exclusiveMinimum() {
        return exclusiveMinimum;
    }

    public void exclusiveMinimum(final @Nullable Boolean exclusiveMinimum) {
        this.exclusiveMinimum = exclusiveMinimum;
    }

    public @Nullable Number maximum() {
        return maximum;
    }

    public void maximum(final @Nullable Number maximum) {
        this.maximum = maximum;
    }

    public @Nullable Boolean exclusiveMaximum() {
        return exclusiveMaximum;
    }

    public void exclusiveMaximum(final @Nullable Boolean exclusiveMaximum) {
        this.exclusiveMaximum = exclusiveMaximum;
    }

    public @Nullable Integer minLength() {
        return minLength;
    }

    public void minLength(final @Nullable Integer minLength) {
        this.minLength = minLength;
    }

    public @Nullable Integer maxLength() {
        return maxLength;
    }

    public void maxLength(final @Nullable Integer maxLength) {
        this.maxLength = maxLength;
    }

    public @Nullable String pattern() {
        return pattern;
    }

    public void pattern(final @Nullable String pattern) {
        this.pattern = pattern;
    }

    public @Nullable Integer minItems() {
        return minItems;
    }

    public void minItems(final @Nullable Integer minItems) {
        this.minItems = minItems;
    }

    public @Nullable Integer maxItems() {
        return maxItems;
    }

    public void maxItems(final @Nullable Integer maxItems) {
        this.maxItems = maxItems;
    }

    public @Nullable Boolean uniqueItems() {
        return uniqueItems;
    }

    public void uniqueItems(final @Nullable Boolean uniqueItems) {
        this.uniqueItems = uniqueItems;
    }

    public @Nullable List<Object> enums() {
        return enums;
    }

    public void enums(final @Nullable List<Object> enums) {
        this.enums = enums;
    }

    public @Nullable Object allowedValue() {
        return allowedValue;
    }

    public void allowedValue(final @Nullable Object allowedValue) {
        this.allowedValue = allowedValue;
    }

    public @Nullable Digits digits() {
        return digits;
    }

    public void digits(final @Nullable Digits digits) {
        this.digits = digits;
    }
}
