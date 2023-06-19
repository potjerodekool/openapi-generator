package io.github.potjerodekool.openapi.internal;

import io.github.potjerodekool.codegen.*;
import io.github.potjerodekool.codegen.kotlin.JavaToKotlinConverter;
import io.github.potjerodekool.codegen.loader.asm.ClassPath;
import io.github.potjerodekool.codegen.model.CompilationUnit;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.element.NestingKind;
import io.github.potjerodekool.codegen.model.symbol.ClassSymbol;
import io.github.potjerodekool.codegen.model.symbol.MethodSymbol;
import io.github.potjerodekool.codegen.model.symbol.PackageSymbol;
import io.github.potjerodekool.codegen.model.symbol.VariableSymbol;
import io.github.potjerodekool.codegen.model.tree.MethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.expression.BinaryExpression;
import io.github.potjerodekool.codegen.model.tree.expression.FieldAccessExpression;
import io.github.potjerodekool.codegen.model.tree.expression.NameExpression;
import io.github.potjerodekool.codegen.model.tree.expression.Operator;
import io.github.potjerodekool.codegen.model.tree.statement.BlockStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.ReturnStatement;
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.NoTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.ParameterizedType;
import io.github.potjerodekool.codegen.model.type.TypeKind;
import io.github.potjerodekool.codegen.model.util.Elements;
import io.github.potjerodekool.codegen.model.util.type.JavaTypes;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.media.SchemaAnnotationBuilder;
import io.github.potjerodekool.openapi.internal.util.TypeUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Set;

@Disabled
class AstPrinterTest {

    private CompilationUnit createPersonModel(final Elements elementUtils) {
        final var clazz = new ClassDeclaration(
                Name.of("Person"),
                ElementKind.CLASS,
                Set.of(),
                List.of()
        );

        clazz.addModifier(Modifier.PUBLIC);

        final var stringType = new ParameterizedType(new NameExpression("java.lang.String"));

        final var field = new VariableDeclaration(
                ElementKind.FIELD,
                Set.of(Modifier.PRIVATE, Modifier.FINAL),
                stringType,
                "name",
                null,
                null
        );

        final var schemaAnnotation = new SchemaAnnotationBuilder()
                .requiredMode(true)
                        .build();

        field.addAnnotation(schemaAnnotation);

        clazz.addEnclosed(field);

        final var constructor = new MethodDeclaration(
                clazz.getSimpleName(),
                ElementKind.CONSTRUCTOR,
                new NoTypeExpression(TypeKind.VOID),
                List.of(),
                List.of(),
                null
        );

        constructor.addParameter(new VariableDeclaration(
                ElementKind.PARAMETER,
                Set.of(Modifier.FINAL),
                stringType,
                "name",
                null,
                null
        ));

        clazz.addEnclosed(constructor);

        final var body = new BlockStatement();
        body.add(new BinaryExpression(
                new FieldAccessExpression(new NameExpression("this"), "name"),
                new NameExpression("name"),
                Operator.ASSIGN
        ));
        constructor.setBody(body);

        final var getter = new MethodDeclaration(
                Name.of("getName"),
                ElementKind.METHOD,
                stringType,
                List.of(),
                List.of(),
                null
        );

        getter.addModifiers(Modifier.PUBLIC);

        getter.setBody(new BlockStatement(
                new ReturnStatement(
                        new FieldAccessExpression(
                                new NameExpression("this"),
                                "name"
                        )
                )
        ));

        clazz.addEnclosed(getter);

        final var cu = new CompilationUnit(Language.JAVA);
        cu.setPackageElement(PackageSymbol.create(Name.of("org.some.models")));
        cu.add(clazz);

        return cu;
    }

    @Test
    void printJavaAst() throws IOException {
        final var environment = new Environment(ClassPath.getJavaClassPath());
        final var cu = createPersonModel(environment.getElementUtils());
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
    @Disabled("Fix find kotlin classes")
    void printKotlinAst() throws IOException {
        final var environment = new Environment(ClassPath.getJavaClassPath());

        var cu = createPersonModel(environment.getElementUtils());

        cu = new JavaToKotlinConverter(environment.getElementUtils(), environment.getTypes()).convert(cu);

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