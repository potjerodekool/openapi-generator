package io.github.potjerodekool.openapi.internal.generate.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.io.Filer;
import io.github.potjerodekool.codegen.model.CompilationUnit;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.tree.PackageDeclaration;
import io.github.potjerodekool.codegen.model.tree.expression.*;
import io.github.potjerodekool.codegen.model.tree.statement.*;
import io.github.potjerodekool.codegen.model.tree.type.AnnotatedTypeExpression;
import io.github.potjerodekool.codegen.model.util.SymbolTable;
import io.github.potjerodekool.openapi.Features;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.internal.ClassNames;
import io.github.potjerodekool.openapi.internal.generate.BasicResolver;
import io.github.potjerodekool.openapi.internal.generate.FullResolver;
import io.github.potjerodekool.openapi.log.LogLevel;
import io.github.potjerodekool.openapi.log.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public class UtilsGenerator {

    private static final Logger LOGGER = Logger.getLogger(UtilsGenerator.class.getName());

    private final SymbolTable symbolTable;
    private final Filer filer;

    private final Language language;
    private final String basePackageName;
    private final String httpServletClassName;
    private final BasicResolver basicResolver;
    private final FullResolver fullResolver;

    public UtilsGenerator(final GeneratorConfig generatorConfig,
                          final Environment environment) {
        this.symbolTable = environment.getSymbolTable();
        this.filer = environment.getFiler();
        this.language = generatorConfig.language();
        this.basePackageName = generatorConfig.basePackageName();

        this.httpServletClassName = generatorConfig.isFeatureEnabled(Features.FEATURE_JAKARTA)
                ? ClassNames.JAKARTA_HTTP_SERVLET_REQUEST
                : ClassNames.JAVA_HTTP_SERVLET_REQUEST;

        this.basicResolver = new BasicResolver(
                environment.getElementUtils(),
                environment.getTypes(),
                environment.getSymbolTable()
        );
        this.fullResolver = new FullResolver(
                environment.getElementUtils(),
                environment.getTypes()
        );
    }

    public void generate() {
        var packageName = this.basePackageName;

        //Try to make the package name end with .api
        if (!packageName.endsWith(".api")) {
            packageName += ".api";
        }

        final var cu = new CompilationUnit(Language.JAVA);

        final var packageDeclaration = new PackageDeclaration(new NameExpression(packageName));
        final var packageSymbol = symbolTable.findOrCreatePackageSymbol(Name.of(packageName));
        packageDeclaration.setPackageSymbol(packageSymbol);
        cu.setPackageElement(packageSymbol);

        final var classDeclaration = new ClassDeclaration(
                Name.of("ApiUtils"),
                ElementKind.CLASS,
                Set.of(Modifier.PUBLIC, Modifier.FINAL),
                List.of()
        );
        cu.add(classDeclaration);

        final var constructor = classDeclaration.addConstructor(Set.of(Modifier.PRIVATE));
        constructor.setBody(new BlockStatement());

        final var createLocationMethod = classDeclaration.addMethod("createLocation", Set.of(Modifier.PUBLIC, Modifier.STATIC));

        final var returnType = new AnnotatedTypeExpression(
                new NameExpression("java.net.URI"),
                List.of()
        );

        createLocationMethod.setReturnType(returnType);

        createLocationMethod.addParameter(new VariableDeclaration(
                ElementKind.PARAMETER,
                Set.of(Modifier.FINAL),
                new AnnotatedTypeExpression(
                        new NameExpression(httpServletClassName),
                        List.of()
                ),
                "request",
                null,
                null
        ));

        createLocationMethod.addParameter(new VariableDeclaration(
                ElementKind.PARAMETER,
                Set.of(Modifier.FINAL),
                new AnnotatedTypeExpression(
                        new NameExpression("java.lang.Object"),
                        List.of()
                ),
                "id",
                null,
                null
        ));

        // final StringBuffer location = request.getRequestURL();
        final var body = new BlockStatement();
        body.add(
                new VariableDeclaration(
                        ElementKind.LOCAL_VARIABLE,
                        Set.of(Modifier.FINAL),
                        new NameExpression("java.lang.StringBuffer"),
                        "locationBuffer",
                        new MethodCallExpression(
                                new NameExpression("request"),
                                "getRequestURL"
                        ),
                        null
                )
        );

        body.add(
                new IfStatement(
                        new BinaryExpression(
                                new MethodCallExpression(
                                        new NameExpression("locationBuffer"),
                                        "charAt",
                                        List.of(
                                                new BinaryExpression(
                                                        new MethodCallExpression(
                                                                new NameExpression("locationBuffer"),
                                                                "length"
                                                        ),
                                                        LiteralExpression.createIntLiteralExpression("1"),
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
                                                new NameExpression("locationBuffer"),
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
                                new NameExpression("java.net.URI"),
                                "create",
                                List.of(
                                        new MethodCallExpression(
                                                new MethodCallExpression(
                                                        new NameExpression("locationBuffer"),
                                                        "append",
                                                        List.of(new NameExpression("id"))
                                                ),
                                                "toString"
                                        )
                                )
                        )
                )
        );

        createLocationMethod.setBody(body);

        basicResolver.resolve(classDeclaration);
        fullResolver.resolve(classDeclaration);
        symbolTable.addClass(classDeclaration.getClassSymbol());

        try {
            filer.writeSource(cu, language);
        } catch (final IOException e) {
            LOGGER.log(LogLevel.SEVERE, "Fail to generate code for ApiUtils", e);
        }
    }
}
