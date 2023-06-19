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
import io.github.potjerodekool.openapi.generate.config.ApiConfigGenerator;
import io.github.potjerodekool.openapi.log.LogLevel;
import io.github.potjerodekool.openapi.log.Logger;
import io.github.potjerodekool.openapi.tree.OpenApi;

import java.io.IOException;

public abstract class AbstractSpringApiConfigGenerator implements ApiConfigGenerator {

    private static final Logger LOGGER = Logger.getLogger(AbstractSpringApiConfigGenerator.class.getName());

    private final GeneratorConfig generatorConfig;
    private final Environment environment;
    private final SymbolTable symbolTable;

    public AbstractSpringApiConfigGenerator(final GeneratorConfig generatorConfig,
                                            final Environment environment) {
        this.generatorConfig = generatorConfig;
        this.environment = environment;
        this.symbolTable = environment.getSymbolTable();
    }

    protected Elements getElementUtils() {
        return environment.getElementUtils();
    }

    protected Types getTypes() {
        return environment.getTypes();
    }

    protected abstract String getConfigClassName();

    @Override
    public void generate(final OpenApi api) {
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

        fillClass(api, classSymbol);
        try {
            environment.getFiler().writeSource(cu, generatorConfig.language());
        } catch (final IOException e) {
            LOGGER.log(LogLevel.SEVERE, "Fail to generate code for spring api definition", e);
        }
    }

    protected abstract void fillClass(OpenApi api,
                                      ClassSymbol typeElement);

    protected boolean skipGeneration() {
        return false;
    }
}
