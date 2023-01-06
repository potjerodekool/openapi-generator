package io.github.potjerodekool.openapi.internal.util;

public record QualifiedName(String packageName,
                            String simpleName) {

    public static QualifiedName from(final String className) {
        final var separatorIndex = className.lastIndexOf('.');
        if (separatorIndex < 0) {
            return new QualifiedName("", className);
        } else {
            return new QualifiedName(className.substring(0, separatorIndex), className.substring(separatorIndex + 1));
        }
    }

    @Override
    public String toString() {
        if ("".equals(packageName)) {
            return simpleName;
        } else {
            return packageName + "." + simpleName;
        }
    }
}
