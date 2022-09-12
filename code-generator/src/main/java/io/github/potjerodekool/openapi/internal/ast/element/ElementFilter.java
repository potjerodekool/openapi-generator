package io.github.potjerodekool.openapi.internal.ast.element;

import java.util.ArrayList;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ElementFilter {

    private ElementFilter() {}

    public static Stream<VariableElement> fields(final TypeElement typeElement) {
        return filterEnclosedElements(
                typeElement,
                it -> it.getKind() == ElementKind.FIELD
        ).map(it -> (VariableElement) it);
    }

    public static Stream<MethodElement> constructors(final TypeElement typeElement) {
        return constructors(typeElement, true);
    }

    public static Stream<MethodElement> constructors(final TypeElement typeElement,
                                                     final boolean includePrimaryConstructor) {
        final var constructorsStream = filterEnclosedElements(
                typeElement,
                it -> it.getKind() == ElementKind.CONSTRUCTOR
        ).map(it -> (MethodElement) it);

        final var primaryConstructor = includePrimaryConstructor ?
                typeElement.getPrimaryConstructor() : null;

        if (primaryConstructor == null) {
            return constructorsStream;
        } else {
            final var list = new ArrayList<MethodElement>();
            list.add(primaryConstructor);
            list.addAll(constructorsStream.toList());
            return list.stream();
        }
    }

    public static Stream<MethodElement> methods(final TypeElement typeElement) {
        return filterEnclosedElements(typeElement, it -> it.getKind() == ElementKind.METHOD)
                .map(it -> (MethodElement) it);
    }

    private static Stream<Element> filterEnclosedElements(final TypeElement element,
                                                          final Predicate<Element> filter) {
        return element.getEnclosedElements().stream()
                .filter(filter);
    }
}
