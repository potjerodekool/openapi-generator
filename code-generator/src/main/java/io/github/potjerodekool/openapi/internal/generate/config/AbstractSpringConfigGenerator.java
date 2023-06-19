package io.github.potjerodekool.openapi.internal.generate.config;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.model.Attribute;
import io.github.potjerodekool.codegen.model.CompilationUnit;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.element.NestingKind;
import io.github.potjerodekool.codegen.model.symbol.ClassSymbol;
import io.github.potjerodekool.codegen.model.util.Elements;
import io.github.potjerodekool.codegen.model.util.SymbolTable;
import io.github.potjerodekool.codegen.model.util.type.JavaTypes;
import io.github.potjerodekool.codegen.model.util.type.Types;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.generate.config.ConfigGenerator;
import io.github.potjerodekool.openapi.log.LogLevel;
import io.github.potjerodekool.openapi.log.Logger;

import java.io.IOException;

public abstract class AbstractSpringConfigGenerator implements ConfigGenerator {

    private static final Logger LOGGER = Logger.getLogger(AbstractSpringConfigGenerator.class.getName());

    private final GeneratorConfig generatorConfig;
    private final Environment environment;
    private final SymbolTable symbolTable;

    public AbstractSpringConfigGenerator(final GeneratorConfig generatorConfig,
                                         final Environment environment) {
        this.generatorConfig = generatorConfig;
        this.environment = environment;
        this.symbolTable = environment.getSymbolTable();
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
        final var packageSymbol = symbolTable.findOrCreatePackageSymbol(Name.of(configPackageName));
        cu.setPackageElement(packageSymbol);

        final var classSymbol = symbolTable.enterClass(ElementKind.CLASS, Name.of(getConfigClassName()), NestingKind.TOP_LEVEL, packageSymbol);
        cu.addElement(classSymbol);

        classSymbol.addAnnotation(Attribute.compound((ClassSymbol) getElementUtils().getTypeElement("org.springframework.context.annotation.Configuration")));

        fillClass(classSymbol);
        try {
            environment.getFiler().writeSource(cu, generatorConfig.language());
        } catch (final IOException e) {
            LOGGER.log(LogLevel.SEVERE, "Fail to generate code for spring api definition", e);
        }
    }

    protected abstract void fillClass(ClassSymbol typeElement);

    protected boolean skipGeneration() {
        return false;
    }
}
