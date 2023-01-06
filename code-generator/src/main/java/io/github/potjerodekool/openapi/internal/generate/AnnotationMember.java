package io.github.potjerodekool.openapi.internal.generate;

import io.github.potjerodekool.openapi.internal.ast.element.AnnotationValue;
import io.github.potjerodekool.openapi.internal.ast.expression.*;
import io.github.potjerodekool.openapi.internal.ast.type.Type;

public class AnnotationMember {

    private final String name;
    private final AnnotationValue value;

    public AnnotationMember(final String name,
                            final AnnotationValue value) {
        this.name = name;
        this.value = value;
    }

    public String name() {
        return name;
    }

    public AnnotationValue value() {
        return value;
    }
}

