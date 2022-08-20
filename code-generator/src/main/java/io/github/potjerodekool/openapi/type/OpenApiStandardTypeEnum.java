package io.github.potjerodekool.openapi.type;

import java.util.Arrays;

public enum OpenApiStandardTypeEnum {

    STRING("string"),
    INTEGER("integer"),
    BOOLEAN("boolean"),
    NUMBER("number"),
    BYTE("byte"),
    SHORT("short");

    private final String typeName;

    OpenApiStandardTypeEnum(final String typeName) {
        this.typeName = typeName;
    }

    public String getTypeName() {
        return typeName;
    }

    public static OpenApiStandardTypeEnum fromType(final String type) {
        return Arrays.stream(values())
                .filter(it -> it.typeName.equals(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(String.format("unknown standard type %s", type)));
    }

    public static boolean isStandardType(final String type) {
        return Arrays.stream(values())
                .anyMatch(it -> it.typeName.equals(type));
    }
}
