package io.github.potjerodekool.openapi;

import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.template.TemplateBasedGenerator;
import io.github.potjerodekool.codegen.template.model.TCompilationUnit;
import io.github.potjerodekool.codegen.template.model.annotation.Annot;
import io.github.potjerodekool.codegen.template.model.element.TypeElem;
import io.github.potjerodekool.codegen.template.model.expression.SimpleLiteralExpr;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CodeGenTest {

    @Test
    void test() {
        final var generator = new TemplateBasedGenerator();
        final var cu = new TCompilationUnit(Language.JAVA);
        cu.packageName("org.some.api");

        final var date = DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now());

        final var typeElem = new TypeElem()
                .kind(ElementKind.INTERFACE)
                .modifier(Modifier.PUBLIC)
                .annotation(
                        new Annot("javax.annotation.processing.Generated")
                                .value(new SimpleLiteralExpr(getClass().getName()))
                                .value("date", new SimpleLiteralExpr(date))
                )
                .simpleName("MyServiceApi");

        cu.element(typeElem);
        final var code = generator.doGenerate(cu);

        System.out.println(code);
    }
}
