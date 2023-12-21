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
import io.github.potjerodekool.codegen.model.tree.statement.IfStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ReturnStatement;
import io.github.potjerodekool.codegen.model.tree.statement.java.JClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.java.JVariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.codegen.template.TemplateBasedGenerator;
import io.github.potjerodekool.openapi.Features;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.internal.ClassNames;

public class ApiUtilsGenerator extends TemplateBasedGenerator {

    private final String basePackageName;
    private final String httpServletClassName;
    private final Language language;

    public ApiUtilsGenerator(final GeneratorConfig generatorConfig,
                             final Environment environment) {
        super(environment);
        this.basePackageName = generatorConfig.basePackageName();
        this.httpServletClassName = resolveHttpServletClassName(generatorConfig);
        this.language = generatorConfig.language();
    }

    private static String resolveHttpServletClassName(final GeneratorConfig generatorConfig) {
        return generatorConfig == null || generatorConfig.isFeatureEnabled(Features.FEATURE_JAKARTA)
                ? ClassNames.JAKARTA_HTTP_SERVLET_REQUEST
                : ClassNames.JAVA_HTTP_SERVLET_REQUEST;
    }

    @Override
    public void generate() {
        final var cu = new CompilationUnit(Language.JAVA);
        final var packageDeclaration = new PackageDeclaration(new IdentifierExpression(basePackageName));

        cu.add(packageDeclaration);

        final var clazz = new JClassDeclaration("ApiUtils", ElementKind.CLASS)
                .modifiers(Modifier.PUBLIC, Modifier.FINAL)
                .annotation(new AnnotationExpression("javax.annotation.processing.Generated",
                        new ArrayInitializerExpression(LiteralExpression.createStringLiteralExpression(getClass().getName()))));

        clazz.setEnclosing(packageDeclaration);

        clazz.addConstructor()
                .modifiers(Modifier.PRIVATE);
        addCreateLocationMethod(clazz);

        cu.add(clazz);

        if (language == Language.JAVA) {
            doGenerate(cu, "ApiUtils.java", language);
        } else if (language == Language.KOTLIN) {
            doGenerate(cu, "ApiUtils.kt", language);
        }
    }

    private void addCreateLocationMethod(final JClassDeclaration clazz) {
            final var method = clazz.addMethod();
            method.setSimpleName(Name.of("createLocation"));
            method.modifiers(Modifier.PUBLIC, Modifier.STATIC);
            method.setReturnType(new ClassOrInterfaceTypeExpression("java.net.URI"));

            method.addParameter(new JVariableDeclaration(ElementKind.PARAMETER)
                    .name("request")
                    .varType(new ClassOrInterfaceTypeExpression(httpServletClassName))
                    .modifier(Modifier.FINAL));

            method.addParameter(new JVariableDeclaration(ElementKind.PARAMETER)
                    .name("id")
                    .varType(new ClassOrInterfaceTypeExpression("java.lang.Object"))
                    .modifier(Modifier.FINAL)
            );

            final var body = new BlockStatement();
            body.add(new JVariableDeclaration(ElementKind.LOCAL_VARIABLE)
                    .varType(new ClassOrInterfaceTypeExpression("java.lang.String"))
                    .name("location")
                    .initExpression(new MethodCallExpression(
                            new MethodCallExpression(
                                    new IdentifierExpression("request"),
                                    "getRequestURL"
                            ),
                            "toString"
                    ))
            );

            body.add(new IfStatement()
                    .condition(new UnaryExpression()
                            .operator(Operator.NOT)
                            .expression(new MethodCallExpression()
                                    .target(new IdentifierExpression("location"))
                                    .methodName("endsWith")
                                    .argument(LiteralExpression.createStringLiteralExpression("/"))
                            )
                    ).body(
                            new BlockStatement(
                                    new BinaryExpression()
                                            .left(new IdentifierExpression("location"))
                                            .operator(Operator.ADD)
                                            .right(LiteralExpression.createStringLiteralExpression("/"))
                            )
                    )
            );

            body.add(new ReturnStatement(
                new MethodCallExpression()
                        .target(new ClassOrInterfaceTypeExpression("java.net.URI"))
                        .methodName("create")
                        .argument(new BinaryExpression()
                                .left(new IdentifierExpression("location"))
                                .operator(Operator.PLUS)
                                .right(new IdentifierExpression("id"))
                        )
            ));

            method.setBody(body);
    }
}
