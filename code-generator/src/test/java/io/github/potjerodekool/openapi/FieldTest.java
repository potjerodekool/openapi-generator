package io.github.potjerodekool.openapi;

import io.github.potjerodekool.codegen.template.model.annotation.Annot;
import io.github.potjerodekool.codegen.template.model.type.ClassOrInterfaceTypeExpr;
import io.github.potjerodekool.openapi.common.generate.Templates;
import org.junit.jupiter.api.Test;

public class FieldTest {

    @Test
    void test() {
        final Templates templates = new Templates();
        final var typeTemplate = templates.getInstanceOf("type");

        final var type = new ClassOrInterfaceTypeExpr()
                .packageName("java.lang")
                .simpleName("String");

        type.annotation(
                new Annot()
                        .name("org.checkerframework.checker.nullness.qual.Nullable")
        );

        typeTemplate.add("type", type);
        final var code = typeTemplate.render();

        System.out.println(code);
    }
}
