package io.github.potjerodekool.openapi.internal.generate.config;

import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.Language;
import io.github.potjerodekool.openapi.generate.config.ApiConfigGenerator;
import io.github.potjerodekool.openapi.internal.Filer;
import io.github.potjerodekool.openapi.internal.ast.Attribute;
import io.github.potjerodekool.openapi.internal.ast.CompilationUnit;
import io.github.potjerodekool.openapi.internal.ast.util.TypeUtils;
import io.github.potjerodekool.openapi.internal.ast.element.PackageElement;
import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;
import io.github.potjerodekool.openapi.log.LogLevel;
import io.github.potjerodekool.openapi.log.Logger;
import io.github.potjerodekool.openapi.tree.OpenApi;

import java.io.IOException;

public abstract class AbstractSpringApiConfigGenerator implements ApiConfigGenerator {

    private static final Logger LOGGER = Logger.getLogger(AbstractSpringApiConfigGenerator.class.getName());

    private final GeneratorConfig generatorConfig;
    private final TypeUtils typeUtils;
    private final Filer filer;

    public AbstractSpringApiConfigGenerator(final GeneratorConfig generatorConfig,
                                            final TypeUtils typeUtils,
                                            final Filer filer) {
        this.generatorConfig = generatorConfig;
        this.typeUtils = typeUtils;
        this.filer = filer;
    }

    protected TypeUtils getTypeUtils() {
        return typeUtils;
    }

    protected abstract String getConfigClassName();

    @Override
    public void generate(final OpenApi api) {
        if (skipGeneration()) {
            return;
        }

        final var cu = new CompilationUnit(Language.JAVA);

        final var configPackageName = generatorConfig.configPackageName();
        cu.setPackageElement(PackageElement.create(configPackageName));

        final var clazz = cu.addClass(getConfigClassName());
        clazz.addAnnotation(Attribute.compound("org.springframework.context.annotation.Configuration"));

        fillClass(api, clazz);
        try {
            filer.writeSource(cu, generatorConfig.language());
        } catch (final IOException e) {
            LOGGER.log(LogLevel.SEVERE, "Fail to generate code for spring api definition", e);
        }
    }

    protected abstract void fillClass(OpenApi api,
                                      TypeElement typeElement);

    protected boolean skipGeneration() {
        return false;
    }
}
