package io.github.potjerodekool.openapi.internal.generate.model;

import java.util.Map;
import java.util.Optional;

public final class ValidationExtensions {
    private ValidationExtensions() {
    }

    public static Optional<Digits> digits(final Map<String, Object> extensions) {
        if (extensions == null) {
            return Optional.empty();
        }

        final var validation = getValidation(extensions);
        final var digits = validation.get("digits");

        if (!(digits instanceof Map)) {
            return Optional.empty();
        }

        final var digitsMap = (Map<String, Object>) digits;

        final var integer = (Integer) digitsMap.get("integer");
        final var fraction = (Integer) digitsMap.get("fraction");
        return Optional.of(new Digits(integer, fraction));
    }

    public static Object allowedValue(final Map<String, Object> extensions) {
        final var validation = getValidation(extensions);
        return validation.get("allowed-value");
    }

    public static Map<String, Object> getValidation(final Map<String, Object> extensions) {
        return (Map<String, Object>) extensions.getOrDefault("x-validation", Map.of());
    }
}
