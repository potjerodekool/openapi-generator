package io.github.potjerodekool.openapi.internal;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.io.Printer;
import io.github.potjerodekool.codegen.kotlin.JavaToKotlinConverter;
import io.github.potjerodekool.codegen.loader.java.ClassPath;
import io.github.potjerodekool.codegen.model.CompilationUnit;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.tree.PackageDeclaration;
import io.github.potjerodekool.codegen.model.tree.expression.BinaryExpression;
import io.github.potjerodekool.codegen.model.tree.expression.FieldAccessExpression;
import io.github.potjerodekool.codegen.model.tree.expression.IdentifierExpression;
import io.github.potjerodekool.codegen.model.tree.expression.Operator;
import io.github.potjerodekool.codegen.model.tree.java.JMethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.BlockStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ReturnStatement;
import io.github.potjerodekool.codegen.model.tree.statement.java.JClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.java.JVariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.NoTypeExpression;
import io.github.potjerodekool.codegen.model.type.TypeKind;
import io.github.potjerodekool.codegen.model.util.SymbolTable;
import io.github.potjerodekool.openapi.internal.generate.annotation.openapi.media.SchemaAnnotationBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Set;

@Disabled
class AstPrinterTest {

    private CompilationUnit createPersonModel(final SymbolTable symbolTable) {
        final var clazz = new JClassDeclaration(
                Name.of("Person"),
                ElementKind.CLASS
        ).modifier(Modifier.PUBLIC);

        final var stringType = new ClassOrInterfaceTypeExpression("java.lang.String");

        final var field = new JVariableDeclaration(
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

        field.annotation(schemaAnnotation);

        clazz.addEnclosed(field);

        final var constructor = new JMethodDeclaration(
                clazz.getSimpleName(),
                ElementKind.CONSTRUCTOR,
                new NoTypeExpression(TypeKind.VOID),
                List.of(),
                List.of(),
                null
        );

        constructor.addParameter(new JVariableDeclaration(
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
                new FieldAccessExpression(new IdentifierExpression("this"), "name"),
                new IdentifierExpression("name"),
                Operator.ASSIGN
        ));
        constructor.setBody(body);

        final var getter = new JMethodDeclaration(
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
                                new IdentifierExpression("this"),
                                "name"
                        )
                )
        ));

        clazz.addEnclosed(getter);

        final var cu = new CompilationUnit(Language.JAVA);
        final var packageSymbol = symbolTable.enterPackage(null, Name.of("org.some.models"));
        final var packageDeclaration = new PackageDeclaration(new IdentifierExpression("org.some.models"));
        packageDeclaration.setPackageSymbol(packageSymbol);
        cu.add(packageDeclaration);
        cu.add(clazz);

        return cu;
    }

    @Test
    void printJavaAst() throws IOException {
        final var environment = new Environment(ClassPath.getJavaClassPath());
        final var cu = createPersonModel(environment.getSymbolTable());
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

        var cu = createPersonModel(environment.getSymbolTable());

        cu = new JavaToKotlinConverter(
                environment.getElementUtils(),
                environment.getTypes()).convert(cu);

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