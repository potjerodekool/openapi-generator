package io.github.potjerodekool.openapi.internal;

import io.github.potjerodekool.openapi.Language;
import io.github.potjerodekool.openapi.internal.ast.*;
import io.github.potjerodekool.openapi.internal.ast.element.MethodElement;
import io.github.potjerodekool.openapi.internal.ast.element.PackageElement;
import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;
import io.github.potjerodekool.openapi.internal.ast.element.VariableElement;
import io.github.potjerodekool.openapi.internal.ast.expression.*;
import io.github.potjerodekool.openapi.internal.ast.statement.BlockStatement;
import io.github.potjerodekool.openapi.internal.ast.statement.ReturnStatement;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;

class AstPrinterTest {

    private CompilationUnit createPersonModel(final TypeUtils types) {
        final var clazz = TypeElement.createClass("Person");

        clazz.addModifier(Modifier.PUBLIC);

        final var stringType = types.createStringType();

        final var field = VariableElement.createField("name", stringType)
                    .addModifiers(Modifier.PRIVATE, Modifier.FINAL);

        final var annotation = new AnnotationExpression("io.swagger.v3.oas.annotations.media.Schema");

        annotation.addElementValue("implementation", LiteralExpression.createClassLiteralExpression(Integer.class));
        annotation.addElementValue("required", LiteralExpression.createBooleanLiteralExpression(true));
        field.addAnnotation(annotation);

        clazz.addEnclosedElement(field);

        final var constructor = MethodElement.createConstructor(clazz.getSimpleName());
        constructor.addParameter(
                VariableElement.createParameter(
                        "name",
                        stringType
                ).addModifiers(Modifier.FINAL)
        );
        clazz.addEnclosedElement(constructor);

        final var body = new BlockStatement();
        body.add(new BinaryExpression(
                new FieldAccessExpression(new NameExpression("this"), "name"),
                new NameExpression("name"),
                Operator.ASSIGN
        ));
        constructor.setBody(body);

        final var getter = MethodElement.createMethod("getName");
        getter.addModifiers(Modifier.PUBLIC);

        getter.setReturnType(stringType);

        getter.setBody(new BlockStatement(
                new ReturnStatement(
                        new FieldAccessExpression(
                                new NameExpression("this"),
                                "name"
                        )
                )
        ));

        clazz.addEnclosedElement(getter);

        final var cu = new CompilationUnit(Language.JAVA);
        cu.setPackageElement(PackageElement.create("org.some.models"));
        cu.addElement(clazz);

        return cu;
    }

    @Test
    void printJavaAst() throws IOException {
        final var typeUtils = new TypeUtils(getClass().getClassLoader());
        final var cu = createPersonModel(typeUtils);
        //final var irCu = new ImportOrganiser().organiseImports(cu);

        final var writer = new BufferedWriter(new OutputStreamWriter(System.out));
        final var printer = new Printer(writer, true);
        //final var irPrinter = new JavaIrPrinter(printer);
        //irCu.accept(irPrinter, irCu);
        //final var astPrinter = new JavaAstPrinter<>(printer);
        //cu.accept(astPrinter, cu);
        writer.flush();
    }

    @Test
    void printKotlinAst() throws IOException {
        final var typeUtils = new TypeUtils(getClass().getClassLoader());
        var cu = createPersonModel(typeUtils);
        cu = new JavaToKotlinConverter(typeUtils).convert(cu);

        //final var irCu = new ImportOrganiser().organiseImports(cu);

        final var writer = new BufferedWriter(new OutputStreamWriter(System.out));
        final var printer = new Printer(writer, true);
        //final var irPrinter = new KotlinIrPrinter(printer);
        //irCu.accept(irPrinter, irCu);

        //final var astPrinter = new KotlinAstPrinter<>(printer);
        //cu.accept(astPrinter, cu);
        writer.flush();
    }

}