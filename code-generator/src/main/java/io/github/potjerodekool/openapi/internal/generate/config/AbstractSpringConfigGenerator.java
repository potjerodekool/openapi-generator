package io.github.potjerodekool.openapi.internal.generate.config;

import io.github.potjerodekool.openapi.Language;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.generate.config.ConfigGenerator;
import io.github.potjerodekool.openapi.internal.Filer;
import io.github.potjerodekool.openapi.internal.ast.CompilationUnit;
import io.github.potjerodekool.openapi.internal.ast.TypeUtils;
import io.github.potjerodekool.openapi.internal.ast.element.PackageElement;
import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;
import io.github.potjerodekool.openapi.log.LogLevel;
import io.github.potjerodekool.openapi.log.Logger;
import io.github.potjerodekool.openapi.tree.OpenApi;

import java.io.IOException;

public abstract class AbstractSpringConfigGenerator implements ConfigGenerator {

    private static final Logger LOGGER = Logger.getLogger(AbstractSpringConfigGenerator.class.getName());

    private final OpenApiGeneratorConfig config;
    private final TypeUtils typeUtils;
    private final Filer filer;

    public AbstractSpringConfigGenerator(final OpenApiGeneratorConfig config,
                                         final TypeUtils typeUtils,
                                         final Filer filer) {
        this.config = config;
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

        final var configPackageName = config.getConfigPackageName();
        cu.setPackageElement(PackageElement.create(configPackageName));

        final var clazz = cu.addClass(getConfigClassName());
        clazz.addAnnotation("org.springframework.context.annotation.Configuration");

        fillClass(api, clazz);
        try {
            filer.write(cu, config.getLanguage());
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
