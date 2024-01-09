package io.github.potjerodekool.openapi.internal.generate.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.model.CompilationUnit;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.PackageDeclaration;
import io.github.potjerodekool.codegen.model.tree.expression.*;
import io.github.potjerodekool.codegen.model.tree.statement.BlockStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ReturnStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ArrayTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.PrimitiveTypeExpression;
import io.github.potjerodekool.openapi.Features;
import io.github.potjerodekool.openapi.GeneratorConfig;

import java.util.List;

public class HttpServletRequestWrapperGenerator {

    private final String basePackageName;
    private final Environment environment;
    private final String httpServletRequestClassName;

    public HttpServletRequestWrapperGenerator(final GeneratorConfig generatorConfig,
                                              final Environment environment) {
        this.basePackageName = generatorConfig.basePackageName();
        this.environment = environment;
        final var basePackageName = generatorConfig.isFeatureEnabled(Features.FEATURE_JAKARTA) ? "jakarta" : "javax";
        httpServletRequestClassName = basePackageName + ".servlet.http.HttpServletRequest";
    }

    public void generate() {
        final var packageName = this.basePackageName;

        final var cu = new CompilationUnit(Language.JAVA);

        final var packageDeclaration = new PackageDeclaration(new IdentifierExpression(packageName));
        cu.packageDeclaration(packageDeclaration);

        final var classDeclaration = new ClassDeclaration()
                .simpleName(Name.of("HttpServletRequestWrapper"))
                .kind(ElementKind.CLASS)
                .modifier(Modifier.PUBLIC)
                        .annotation(new AnnotationExpression("javax.annotation.processing.Generated", LiteralExpression.createStringLiteralExpression(getClass().getName())));

        classDeclaration.addImplement(new ClassOrInterfaceTypeExpression(
                packageName + ".Request"
        ));

        classDeclaration.setEnclosing(packageDeclaration);
        cu.classDeclaration(classDeclaration);

        addRequestField(classDeclaration);
        addConstructor(classDeclaration);

        addGetParameterMethod(classDeclaration);
        addGetParameterValues(classDeclaration);
        addGetParameterMapMethod(classDeclaration);
        addGetRemoteAddrMethod(classDeclaration);
        addGetRemoteHostMethod(classDeclaration);
        addGetRemotePortMethod(classDeclaration);

        environment.getCompilationUnits().add(cu);
    }

    private void addRequestField(final ClassDeclaration classDeclaration) {
        final var field = new VariableDeclaration()
                .kind(ElementKind.FIELD)
                .modifiers(Modifier.PRIVATE, Modifier.FINAL)
                .varType(new ClassOrInterfaceTypeExpression(httpServletRequestClassName))
                .name("request");

        classDeclaration.addEnclosed(field);
    }

    private void addConstructor(final ClassDeclaration classDeclaration) {
        classDeclaration.constructor(constructor -> {
            constructor.modifier(Modifier.PUBLIC);
            constructor.parameter(
                    VariableDeclaration.parameter()
                            .modifier(Modifier.FINAL)
                            .varType(new ClassOrInterfaceTypeExpression(httpServletRequestClassName))
                            .name("request")
            );

            final var constructorBody = new BlockStatement();

            constructorBody.add(
                    new BinaryExpression(
                            new FieldAccessExpression(
                                    new IdentifierExpression("this"),
                                    "request"
                            ),
                            new IdentifierExpression("request"),
                            Operator.ASSIGN
                    )
            );

            constructor.body(constructorBody);
        });
    }

    private void addGetParameterMethod(final ClassDeclaration classDeclaration) {
        classDeclaration.method(method -> {
            method.annotation("java.lang.Override")
                    .modifier(Modifier.PUBLIC)
                    .simpleName(Name.of("getParameter"))
                    .returnType(new ClassOrInterfaceTypeExpression("java.lang.String"))
                    .parameter(
                            VariableDeclaration.parameter()
                                    .varType(new ClassOrInterfaceTypeExpression("java.lang.String"))
                                    .name("name")
                    );

            final var methodCall = new MethodCallExpression()
                    .target(new FieldAccessExpression(
                            new IdentifierExpression("this"),
                            "request"
                    ))
                    .methodName("getParameter")
                    .argument(new IdentifierExpression("name"));

            method.body(new BlockStatement(new ReturnStatement(methodCall)));
        });
    }

    private void addGetParameterValues(final ClassDeclaration classDeclaration) {
        classDeclaration.method(method -> {
            method.annotation("java.lang.Override")
                    .modifier(Modifier.PUBLIC)
                    .simpleName(Name.of("getParameterValues"))
                    .returnType(new ArrayTypeExpression(new ClassOrInterfaceTypeExpression("java.lang.String")))
                    .parameter(
                            VariableDeclaration.parameter()
                                    .varType(new ClassOrInterfaceTypeExpression("java.lang.String"))
                                    .name("name")
                    );

            final var methodCall = new MethodCallExpression()
                    .target(new FieldAccessExpression(
                            new IdentifierExpression("this"),
                            "request"
                    ))
                    .methodName("getParameterValues")
                    .argument(new IdentifierExpression("name"));

            method.body(new BlockStatement(new ReturnStatement(methodCall)));
        });
    }

    private void addGetParameterMapMethod(final ClassDeclaration classDeclaration) {
        classDeclaration.method(method -> {
            method.annotation("java.lang.Override")
                    .modifier(Modifier.PUBLIC)
                    .simpleName(Name.of("getParameterMap"))
                    .returnType(new ClassOrInterfaceTypeExpression(
                            "java.util.Map",
                            List.of(
                                    new ClassOrInterfaceTypeExpression("java.lang.String"),
                                    new ArrayTypeExpression(new ClassOrInterfaceTypeExpression("java.lang.String"))
                            )
                    ));

            final var methodCall = new MethodCallExpression()
                    .target(new FieldAccessExpression(
                            new IdentifierExpression("this"),
                            "request"
                    ))
                    .methodName("getParameterMap");

            method.body(new BlockStatement(new ReturnStatement(methodCall)));
        });
    }

    private void addGetRemoteAddrMethod(final ClassDeclaration classDeclaration) {
        classDeclaration.method(method -> {
            method.annotation("java.lang.Override")
                    .modifier(Modifier.PUBLIC)
                    .simpleName(Name.of("getRemoteAddr"))
                    .returnType(new ClassOrInterfaceTypeExpression("java.lang.String"));

            final var methodCall = new MethodCallExpression()
                    .target(new FieldAccessExpression(
                            new IdentifierExpression("this"),
                            "request"
                    ))
                    .methodName("getRemoteAddr");

            method.body(new BlockStatement(new ReturnStatement(methodCall)));
        });
    }

    private void addGetRemoteHostMethod(final ClassDeclaration classDeclaration) {
        classDeclaration.method(method -> {
            method.annotation("java.lang.Override")
                    .modifier(Modifier.PUBLIC)
                    .simpleName(Name.of("getRemoteHost"))
                    .returnType(new ClassOrInterfaceTypeExpression("java.lang.String"));

            final var methodCall = new MethodCallExpression()
                    .target(new FieldAccessExpression(
                            new IdentifierExpression("this"),
                            "request"
                    ))
                    .methodName("getRemoteHost");

            method.body(new BlockStatement(new ReturnStatement(methodCall)));
        });
    }

    private void addGetRemotePortMethod(final ClassDeclaration classDeclaration) {
        classDeclaration.method(method -> {
            method.annotation("java.lang.Override")
                    .modifier(Modifier.PUBLIC)
                    .simpleName(Name.of("getRemotePort"))
                    .returnType(PrimitiveTypeExpression.intTypeExpression());

            final var methodCall = new MethodCallExpression()
                    .target(new FieldAccessExpression(
                            new IdentifierExpression("this"),
                            "request"
                    ))
                    .methodName("getRemotePort");

            method.body(new BlockStatement(new ReturnStatement(methodCall)));
        });
    }
}
