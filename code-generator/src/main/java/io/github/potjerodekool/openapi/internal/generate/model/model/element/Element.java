package io.github.potjerodekool.openapi.internal.generate.model.model.element;

import java.util.List;
import java.util.Set;

public interface Element {

    String getSimpleName();

    String getQualifiedName();

    Element getEnclosedElement();

    List<Element> getEnclosedElements();

    Set<Modifier> getModifiers();
}
