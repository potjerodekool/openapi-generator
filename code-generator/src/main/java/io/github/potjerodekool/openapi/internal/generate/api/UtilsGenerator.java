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
import io.github.potjerodekool.codegen.model.tree.statement.ExpressionStatement;
import io.github.potjerodekool.codegen.model.tree.statement.IfStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ReturnStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.NoTypeExpression;
import io.github.potjerodekool.codegen.model.type.TypeKind;
import io.github.potjerodekool.openapi.Features;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.internal.ClassNames;

import java.util.List;

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
        cu.packageDeclaration(packageDeclaration);

        final var classDeclaration = new ClassDeclaration()
                .simpleName(Name.of("ApiUtils"))
                .kind(ElementKind.CLASS)
                .   modifiers(Modifier.PUBLIC, Modifier.FINAL);

        classDeclaration.annotation(new AnnotationExpression("javax.annotation.processing.Generated", LiteralExpression.createStringLiteralExpression(getClass().getName())));
        classDeclaration.setEnclosing(packageDeclaration);
        cu.classDeclaration(classDeclaration);

        classDeclaration.constructor(constructor -> {
            constructor.modifier(Modifier.PRIVATE);
            constructor.body(new BlockStatement());
        });

        classDeclaration.method(method -> {
           method.returnType(new NoTypeExpression(TypeKind.VOID))
                   .simpleName(Name.of("createLocation"))
                   .modifiers(Modifier.PUBLIC, Modifier.STATIC);

            final var returnType = new ClassOrInterfaceTypeExpression("java.net.URI");

            method.returnType(returnType);

            method.parameter(new VariableDeclaration()
                    .kind(ElementKind.PARAMETER)
                    .modifier(Modifier.FINAL)
                    .varType(new ClassOrInterfaceTypeExpression(httpServletClassName))
                    .name("request")
            );

            method.parameter(new VariableDeclaration()
                   .kind(ElementKind.PARAMETER).
                    modifier(Modifier.FINAL)
                    .varType(new ClassOrInterfaceTypeExpression("java.lang.Object"))
                    .name("id")
            );

            // final StringBuffer location = request.getRequestURL();
            final var body = new BlockStatement();
            body.add(
                    new VariableDeclaration()
                            .kind(ElementKind.LOCAL_VARIABLE)
                            .modifier(Modifier.FINAL)
                            .varType(new ClassOrInterfaceTypeExpression("java.lang.StringBuffer"))
                            .name("locationBuffer")
                            .initExpression(new MethodCallExpression(
                                    new IdentifierExpression("request"),
                                    "getRequestURL"
                            ))
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

            method.body(body);
        });

        environment.getCompilationUnits().add(cu);
    }
}
