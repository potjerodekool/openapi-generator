package io.github.potjerodekool.openapi.generate.config;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import io.github.potjerodekool.openapi.Filer;
import io.github.potjerodekool.openapi.LogLevel;
import io.github.potjerodekool.openapi.Logger;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.generate.Types;
import io.github.potjerodekool.openapi.tree.OpenApi;

import java.io.IOException;

public abstract class AbstractSpringConfigGenerator {

    private static final Logger LOGGER = Logger.getLogger(AbstractSpringConfigGenerator.class.getName());

    protected final OpenApiGeneratorConfig config;
    protected final Types types;
    protected final Filer filer;

    public AbstractSpringConfigGenerator(final OpenApiGeneratorConfig config,
                                         final Types types,
                                         final Filer filer) {
        this.config = config;
        this.types = types;
        this.filer = filer;
    }

    protected abstract String getConfigClassName();

    public void generate(final OpenApi api) {
        final var cu = new CompilationUnit();

        final var configPackageName = config.getConfigPackageName();

        if (configPackageName != null) {
            cu.setPackageDeclaration(configPackageName);
        }

        final var clazz = cu.addClass(getConfigClassName());
        clazz.addMarkerAnnotation("org.springframework.context.annotation.Configuration");

        fillClass(api, clazz);
        try {
            filer.write(cu);
        } catch (final IOException e) {
            LOGGER.log(LogLevel.SEVERE, "Fail to generate code for spring api definition", e);
        }
    }

    protected abstract void fillClass(OpenApi api,
                                      ClassOrInterfaceDeclaration clazz);
}