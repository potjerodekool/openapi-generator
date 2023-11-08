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
import io.github.potjerodekool.codegen.model.tree.statement.java.JClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.java.JVariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ArrayTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.PrimitiveTypeExpression;
import io.github.potjerodekool.openapi.Features;
import io.github.potjerodekool.openapi.GeneratorConfig;

import java.util.List;
import java.util.Set;

import static io.github.potjerodekool.codegen.model.tree.expression.MethodCallExpressionBuilder.invoke;

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
        cu.add(packageDeclaration);

        final var classDeclaration = new JClassDeclaration(
                "HttpServletRequestWrapper",
                ElementKind.CLASS
        ).modifier(Modifier.PUBLIC)
                        .annotation(new AnnotationExpression("javax.annotation.processing.Generated", LiteralExpression.createStringLiteralExpression(getClass().getName())));

        classDeclaration.addImplement(new ClassOrInterfaceTypeExpression(
                packageName + ".Request"
        ));

        classDeclaration.setEnclosing(packageDeclaration);
        cu.add(classDeclaration);

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

    private void addRequestField(final JClassDeclaration classDeclaration) {
        final var field = new JVariableDeclaration(
                ElementKind.FIELD,
                Set.of(Modifier.PRIVATE, Modifier.FINAL),
                new ClassOrInterfaceTypeExpression(httpServletRequestClassName),
                "request",
                null,
                null
        );

        classDeclaration.addEnclosed(field);
    }

    private void addConstructor(final JClassDeclaration classDeclaration) {
        final var constructor = classDeclaration.addConstructor();
        constructor.addModifier(Modifier.PUBLIC);
        constructor.addParameter(
                JVariableDeclaration.parameter()
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

        constructor.setBody(constructorBody);
    }

    private void addGetParameterMethod(final JClassDeclaration classDeclaration) {
        final var method = classDeclaration.addMethod()
                .annotation("java.lang.Override")
                .modifier(Modifier.PUBLIC)
                .setSimpleName(Name.of("getParameter"))
                .setReturnType(new ClassOrInterfaceTypeExpression("java.lang.String"))
                .addParameter(
                    JVariableDeclaration.parameter()
                            .varType(new ClassOrInterfaceTypeExpression("java.lang.String"))
                            .name("name")
                );

        final var methodCall = invoke(
                new FieldAccessExpression(
                        new IdentifierExpression("this"),
                        "request"
                ),
                "getParameter"
        ).withArgs(new IdentifierExpression("name"))
                .build();

        method.setBody(new BlockStatement(new ReturnStatement(methodCall)));
    }

    private void addGetParameterValues(final JClassDeclaration classDeclaration) {
        final var method = classDeclaration.addMethod()
                .annotation("java.lang.Override")
                .modifier(Modifier.PUBLIC)
                .setSimpleName(Name.of("getParameterValues"))
                .setReturnType(new ArrayTypeExpression(new ClassOrInterfaceTypeExpression("java.lang.String")))
                .addParameter(
                        JVariableDeclaration.parameter()
                                .varType(new ClassOrInterfaceTypeExpression("java.lang.String"))
                                .name("name")
                );

        final var methodCall = invoke(
                        new FieldAccessExpression(
                                new IdentifierExpression("this"),
                                "request"
                        ),
                        "getParameterValues"
                ).withArgs(new IdentifierExpression("name"))
                .build();

        method.setBody(new BlockStatement(new ReturnStatement(methodCall)));
    }

    private void addGetParameterMapMethod(final JClassDeclaration classDeclaration) {
        final var method = classDeclaration.addMethod()
                .annotation("java.lang.Override")
                .modifier(Modifier.PUBLIC)
                .setSimpleName(Name.of("getParameterMap"))
                .setReturnType(new ClassOrInterfaceTypeExpression(
                        "java.util.Map",
                        List.of(
                                new ClassOrInterfaceTypeExpression("java.lang.String"),
                                new ArrayTypeExpression(new ClassOrInterfaceTypeExpression("java.lang.String"))
                        )
                ));

        final var methodCall = invoke(
                        new FieldAccessExpression(
                                new IdentifierExpression("this"),
                                "request"
                        ),
                        "getParameterMap"
                ).build();

        method.setBody(new BlockStatement(new ReturnStatement(methodCall)));
    }

    private void addGetRemoteAddrMethod(final JClassDeclaration classDeclaration) {
        final var method = classDeclaration.addMethod()
                .annotation("java.lang.Override")
                .modifier(Modifier.PUBLIC)
                .setSimpleName(Name.of("getRemoteAddr"))
                .setReturnType(new ClassOrInterfaceTypeExpression("java.lang.String"));

        final var methodCall = invoke(
                new FieldAccessExpression(
                        new IdentifierExpression("this"),
                        "request"
                ),
                "getRemoteAddr"
        ).build();

        method.setBody(new BlockStatement(new ReturnStatement(methodCall)));
    }

    private void addGetRemoteHostMethod(final JClassDeclaration classDeclaration) {
        final var method = classDeclaration.addMethod()
                .annotation("java.lang.Override")
                .modifier(Modifier.PUBLIC)
                .setSimpleName(Name.of("getRemoteHost"))
                .setReturnType(new ClassOrInterfaceTypeExpression("java.lang.String"));

        final var methodCall = invoke(
                new FieldAccessExpression(
                        new IdentifierExpression("this"),
                        "request"
                ),
                "getRemoteHost"
        ).build();

        method.setBody(new BlockStatement(new ReturnStatement(methodCall)));
    }

    private void addGetRemotePortMethod(final JClassDeclaration classDeclaration) {
        final var method = classDeclaration.addMethod()
                .annotation("java.lang.Override")
                .modifier(Modifier.PUBLIC)
                .setSimpleName(Name.of("getRemotePort"))
                .setReturnType(PrimitiveTypeExpression.intTypeExpression());

        final var methodCall = invoke(
                new FieldAccessExpression(
                        new IdentifierExpression("this"),
                        "request"
                ),
                "getRemotePort"
        ).build();

        method.setBody(new BlockStatement(new ReturnStatement(methodCall)));
    }
}
