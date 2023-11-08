package io.github.potjerodekool.openapi.internal.generate.config;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.model.CompilationUnit;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.PackageDeclaration;
import io.github.potjerodekool.codegen.model.tree.expression.IdentifierExpression;
import io.github.potjerodekool.codegen.model.tree.expression.LiteralExpression;
import io.github.potjerodekool.codegen.model.tree.statement.java.JClassDeclaration;
import io.github.potjerodekool.codegen.model.util.Elements;
import io.github.potjerodekool.codegen.model.util.type.Types;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.generate.config.ConfigGenerator;

public abstract class AbstractSpringConfigGenerator implements ConfigGenerator {

    private final GeneratorConfig generatorConfig;
    private final Environment environment;

    public AbstractSpringConfigGenerator(final GeneratorConfig generatorConfig,
                                         final Environment environment) {
        this.generatorConfig = generatorConfig;
        this.environment = environment;
    }

    public Environment getEnvironment() {
        return environment;
    }

    public Elements getElementUtils() {
        return environment.getElementUtils();
    }

    public Types getTypes() {
        return environment.getTypes();
    }

    protected abstract String getConfigClassName();

    @Override
    public void generate() {
        if (skipGeneration()) {
            return;
        }

        final var cu = new CompilationUnit(Language.JAVA);

        final var configPackageName = generatorConfig.configPackageName();
        final var packageDeclaration = new PackageDeclaration(new IdentifierExpression(configPackageName));
        cu.add(packageDeclaration);

        final var classDeclaration = new JClassDeclaration(getConfigClassName(), ElementKind.CLASS)
                .modifier(Modifier.PUBLIC)
                .annotation(new AnnotationExpression("org.springframework.context.annotation.Configuration"))
                .annotation(new AnnotationExpression("javax.annotation.processing.Generated", LiteralExpression.createStringLiteralExpression(getClass().getName())));
        classDeclaration.setEnclosing(packageDeclaration);

        cu.add(classDeclaration);

        fillClass(classDeclaration);

        environment.getCompilationUnits().add(cu);
    }

    protected abstract void fillClass(JClassDeclaration classDeclaration);


    protected boolean skipGeneration() {
        return false;
    }
}
