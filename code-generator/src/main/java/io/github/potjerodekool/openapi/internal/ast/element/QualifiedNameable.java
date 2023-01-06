package io.github.potjerodekool.openapi.internal.ast.element;

public interface QualifiedNameable {

    String getQualifiedName();

    static String getQualifiedName(final Element element) {
        final var stringBuilder = new StringBuilder();
        createQualifiedName(element, stringBuilder);
        return stringBuilder.toString();
    }

    static String getSimpleName(final String name) {
        final var sepIndex = name.lastIndexOf('.');
        return sepIndex < 0 ? name : name.substring(sepIndex + 1);
    }

    private static void createQualifiedName(final Element element,
                                            final StringBuilder stringBuilder) {
        final var enclosingElement = element.getEnclosingElement();

        if (enclosingElement != null && !isDefaultPackage(enclosingElement)) {
            createQualifiedName(enclosingElement, stringBuilder);
            stringBuilder.append(".");
        }

        stringBuilder.append(element.getSimpleName());
    }

    private static boolean isDefaultPackage(final Element element) {
        if (element instanceof PackageElement pe) {
            return pe.isDefaultPackage();
        } else {
            return false;
        }
    }
}
