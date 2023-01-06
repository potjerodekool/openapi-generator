package io.github.potjerodekool.openapi.internal.ast.element;

import io.github.potjerodekool.openapi.internal.ast.type.java.JavaErrorType;

import javax.lang.model.element.ElementKind;

public class ErrorElement extends TypeElement {

    private ErrorElement() {
        super(ElementKind.OTHER, "error");
    }

    public static ErrorElement create() {
        final var element = new ErrorElement();
        final var type = new JavaErrorType(element);
        element.setType(type);
        return element;
    }

    @Override
    public ElementKind getKind() {
        return ElementKind.OTHER;
    }

}
