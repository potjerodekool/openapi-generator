package io.github.potjerodekool.openapi;

import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Arrays;

public enum Language {

    JAVA("java"),
    KOTLIN("kt");

    private final String fileExtension;

    public static Language fromString(final @Nullable String value) {
        if (value == null) {
            return Language.JAVA;
        }

        final var upperCaseValue = value.toUpperCase();
        return Arrays.stream(values())
                .filter(language -> language.name().equals(upperCaseValue))
                .findFirst()
                .orElse(Language.JAVA);
    }

    Language(final String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getFileExtension() {
        return fileExtension;
    }
}
