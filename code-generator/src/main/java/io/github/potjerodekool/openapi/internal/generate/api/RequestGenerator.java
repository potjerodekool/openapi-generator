package io.github.potjerodekool.openapi.internal.generate.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.model.CompilationUnit;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.PackageDeclaration;
import io.github.potjerodekool.codegen.model.tree.expression.IdentifierExpression;
import io.github.potjerodekool.codegen.model.tree.expression.LiteralExpression;
import io.github.potjerodekool.codegen.model.tree.statement.ClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ArrayTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.PrimitiveTypeExpression;
import io.github.potjerodekool.openapi.GeneratorConfig;

import java.util.List;

public class RequestGenerator {

    private final String basePackageName;
    private final Environment environment;

    public RequestGenerator(final GeneratorConfig generatorConfig,
                            final Environment environment) {
        this.basePackageName = generatorConfig.basePackageName();
        this.environment = environment;
    }

    public void generate() {
        final var cu = new CompilationUnit(Language.JAVA);

        final var packageDeclaration = new PackageDeclaration(new IdentifierExpression(this.basePackageName));
        cu.packageDeclaration(packageDeclaration);

        final var classDeclaration = new ClassDeclaration()
                .simpleName(Name.of("Request"))
                .kind(ElementKind.INTERFACE)
                .modifiers(Modifier.PUBLIC)
                        .annotation(new AnnotationExpression("javax.annotation.processing.Generated", LiteralExpression.createStringLiteralExpression(getClass().getName())));

        classDeclaration.setEnclosing(packageDeclaration);
        cu.classDeclaration(classDeclaration);

        addGetParameterMethod(classDeclaration);
        addGetParameterValuesMethod(classDeclaration);
        addGetParameterMapMethod(classDeclaration);
        addGetRemoteAddrMethod(classDeclaration);
        addGetRemoteHostMethod(classDeclaration);
        addGetRemotePortMethod(classDeclaration);

        environment.getCompilationUnits().add(cu);
    }

    private void addGetParameterMethod(final ClassDeclaration classDeclaration) {
        classDeclaration.createMethod()
                .simpleName(Name.of("getParameter"))
                .returnType(new ClassOrInterfaceTypeExpression("java.lang.String"))
                .parameter(
                        VariableDeclaration.parameter()
                                .varType(new ClassOrInterfaceTypeExpression("java.lang.String"))
                                .name("name")
                );
    }

    private void addGetParameterValuesMethod(final ClassDeclaration classDeclaration) {
        classDeclaration.createMethod()
                .simpleName(Name.of("getParameterValues"))
                .returnType(new ArrayTypeExpression(new ClassOrInterfaceTypeExpression("java.lang.String")))
                .parameter(
                        VariableDeclaration.parameter()
                                .varType(new ClassOrInterfaceTypeExpression("java.lang.String"))
                                .name("name")
                );
    }

    private void addGetParameterMapMethod(final ClassDeclaration classDeclaration) {
        classDeclaration.createMethod()
                .simpleName(Name.of("getParameterMap"))
                .returnType(new ClassOrInterfaceTypeExpression(
                        "java.util.Map",
                        List.of(
                                new ClassOrInterfaceTypeExpression("java.lang.String"),
                                new ArrayTypeExpression(new ClassOrInterfaceTypeExpression("java.lang.String"))
                        )
                ));
    }

    private void addGetRemoteAddrMethod(final ClassDeclaration classDeclaration) {
        classDeclaration.createMethod()
                .simpleName(Name.of("getRemoteAddr"))
                .returnType(new ClassOrInterfaceTypeExpression("java.lang.String"));
    }

    private void addGetRemoteHostMethod(final ClassDeclaration classDeclaration) {
        classDeclaration.createMethod()
                .simpleName(Name.of("getRemoteHost"))
                .returnType(new ClassOrInterfaceTypeExpression("java.lang.String"));
    }

    private void addGetRemotePortMethod(final ClassDeclaration classDeclaration) {
        classDeclaration.createMethod()
                .simpleName(Name.of("getRemotePort"))
                .returnType(PrimitiveTypeExpression.intTypeExpression());
    }
}
