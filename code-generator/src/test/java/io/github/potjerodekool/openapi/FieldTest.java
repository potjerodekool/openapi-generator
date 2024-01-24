package io.github.potjerodekool.openapi;

import io.github.potjerodekool.openapi.internal.generate.Templates;
import io.github.potjerodekool.openapi.internal.generate.model.model.element.Annotation;
import io.github.potjerodekool.openapi.internal.generate.model.model.type.ReferenceType;
import org.junit.jupiter.api.Test;

public class FieldTest {

    @Test
    void test() {
        final Templates templates = new Templates();
        final var typeTemplate = templates.getInstanceOf("type");

        final var type = new ReferenceType()
                .name("java.lang.String");

        type.annotation(
                new Annotation()
                        .name("org.checkerframework.checker.nullness.qual.Nullable")
        );

        typeTemplate.add("type", type);
        final var code = typeTemplate.render();

        System.out.println(code);
    }
}
