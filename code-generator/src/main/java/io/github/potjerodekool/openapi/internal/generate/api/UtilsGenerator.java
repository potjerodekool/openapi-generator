package io.github.potjerodekool.openapi.internal.generate.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.model.CompilationUnit;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.PackageDeclaration;
import io.github.potjerodekool.codegen.model.tree.expression.*;
import io.github.potjerodekool.codegen.model.tree.statement.BlockStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ExpressionStatement;
import io.github.potjerodekool.codegen.model.tree.statement.IfStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ReturnStatement;
import io.github.potjerodekool.codegen.model.tree.statement.java.JClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.java.JVariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.NoTypeExpression;
import io.github.potjerodekool.codegen.model.type.TypeKind;
import io.github.potjerodekool.openapi.Features;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.internal.ClassNames;

import java.util.List;
import java.util.Set;

public class UtilsGenerator {

    private final Environment environment;
    private final String basePackageName;
    private final String httpServletClassName;

    public UtilsGenerator(final GeneratorConfig generatorConfig,
                          final Environment environment) {
        this.environment = environment;
        this.basePackageName = generatorConfig.basePackageName();

        this.httpServletClassName = generatorConfig.isFeatureEnabled(Features.FEATURE_JAKARTA)
                ? ClassNames.JAKARTA_HTTP_SERVLET_REQUEST
                : ClassNames.JAVA_HTTP_SERVLET_REQUEST;
    }

    public void generate() {
        final var cu = new CompilationUnit(Language.JAVA);

        final var packageDeclaration = new PackageDeclaration(new IdentifierExpression(this.basePackageName));
        cu.add(packageDeclaration);

        final var classDeclaration = new JClassDeclaration(
                "ApiUtils",
                ElementKind.CLASS
        ).modifiers(Modifier.PUBLIC, Modifier.FINAL);

        classDeclaration.annotation(new AnnotationExpression("javax.annotation.processing.Generated", LiteralExpression.createStringLiteralExpression(getClass().getName())));
        classDeclaration.setEnclosing(packageDeclaration);
        cu.add(classDeclaration);

        final var constructor = classDeclaration.addConstructor();
        constructor.setReturnType(new NoTypeExpression(TypeKind.VOID));
        constructor.addModifiers(Modifier.PRIVATE);

        constructor.setBody(new BlockStatement());

        final var createLocationMethod = classDeclaration.addMethod(
                new NoTypeExpression(TypeKind.VOID),
                "createLocation",
                Set.of(Modifier.PUBLIC,
                        Modifier.STATIC)
        );

        final var returnType = new ClassOrInterfaceTypeExpression("java.net.URI");

        createLocationMethod.setReturnType(returnType);

        createLocationMethod.addParameter(
                new JVariableDeclaration(
                        ElementKind.PARAMETER,
                        Set.of(Modifier.FINAL),
                        new ClassOrInterfaceTypeExpression(httpServletClassName),
                        "request",
                        null,
                        null
                )
        );

        createLocationMethod.addParameter(new JVariableDeclaration(
                ElementKind.PARAMETER,
                Set.of(Modifier.FINAL),
                new ClassOrInterfaceTypeExpression("java.lang.Object"),
                "id",
                null,
                null
        ));

        // final StringBuffer location = request.getRequestURL();
        final var body = new BlockStatement();
        body.add(
                new JVariableDeclaration(
                        ElementKind.LOCAL_VARIABLE,
                        Set.of(Modifier.FINAL),
                        new ClassOrInterfaceTypeExpression("java.lang.StringBuffer"),
                        "locationBuffer",
                        new MethodCallExpression(
                                new IdentifierExpression("request"),
                                "getRequestURL"
                        ),
                        null
                )
        );

        body.add(
                new IfStatement(
                        new BinaryExpression(
                                new MethodCallExpression(
                                        new IdentifierExpression("locationBuffer"),
                                        "charAt",
                                        List.of(
                                                new BinaryExpression(
                                                        new MethodCallExpression(
                                                                new IdentifierExpression("locationBuffer"),
                                                                "length"
                                                        ),
                                                        LiteralExpression.createIntLiteralExpression(1),
                                                        Operator.MINUS
                                                )
                                        )
                                ),
                                LiteralExpression.createCharLiteralExpression('/'),
                                Operator.NOT_EQUALS
                        ),
                        new BlockStatement(
                                new ExpressionStatement(
                                        new MethodCallExpression(
                                                new IdentifierExpression("locationBuffer"),
                                                "append",
                                                List.of(LiteralExpression.createCharLiteralExpression('/'))
                                        )
                                )
                        )
                )
        );

        // return Uri.create(location.append(id).toString())
        body.add(
                new ReturnStatement(
                        new MethodCallExpression(
                                new ClassOrInterfaceTypeExpression("java.net.URI"),
                                "create",
                                List.of(
                                        new MethodCallExpression(
                                                new MethodCallExpression(
                                                        new IdentifierExpression("locationBuffer"),
                                                        "append",
                                                        List.of(new IdentifierExpression("id"))
                                                ),
                                                "toString"
                                        )
                                )
                        )
                )
        );

        createLocationMethod.setBody(body);

        environment.getCompilationUnits().add(cu);
    }
}
