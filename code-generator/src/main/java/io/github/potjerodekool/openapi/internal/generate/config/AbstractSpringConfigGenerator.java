package io.github.potjerodekool.openapi.internal.generate.config;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.internal.Filer;
import io.github.potjerodekool.openapi.LogLevel;
import io.github.potjerodekool.openapi.Logger;
import io.github.potjerodekool.openapi.internal.generate.Types;
import io.github.potjerodekool.openapi.tree.OpenApi;

import java.io.IOException;

public abstract class AbstractSpringConfigGenerator {

    private static final Logger LOGGER = Logger.getLogger(AbstractSpringConfigGenerator.class.getName());

    private final OpenApiGeneratorConfig config;
    private final Types types;
    private final Filer filer;

    public AbstractSpringConfigGenerator(final OpenApiGeneratorConfig config,
                                         final Types types,
                                         final Filer filer) {
        this.config = config;
        this.types = types;
        this.filer = filer;
    }

    public Types getTypes() {
        return types;
    }

    protected abstract String getConfigClassName();

    public void generate(final OpenApi api) {
        if (skipGeneration()) {
            return;
        }

        final var cu = types.createCompilationUnit();

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

    protected boolean skipGeneration() {
        return false;
    }
}
