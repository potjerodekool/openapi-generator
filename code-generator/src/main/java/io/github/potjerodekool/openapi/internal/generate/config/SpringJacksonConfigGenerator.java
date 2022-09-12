package io.github.potjerodekool.openapi.internal.generate.config;

import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.dependency.Artifact;
import io.github.potjerodekool.openapi.dependency.DependencyChecker;
import io.github.potjerodekool.openapi.internal.Filer;
import io.github.potjerodekool.openapi.internal.ast.Modifier;
import io.github.potjerodekool.openapi.internal.ast.TypeUtils;
import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;
import io.github.potjerodekool.openapi.internal.ast.expression.ArrayInitializerExpression;
import io.github.potjerodekool.openapi.internal.ast.expression.LiteralExpression;
import io.github.potjerodekool.openapi.internal.ast.expression.NewClassExpression;
import io.github.potjerodekool.openapi.internal.ast.statement.BlockStatement;
import io.github.potjerodekool.openapi.internal.ast.statement.ReturnStatement;
import io.github.potjerodekool.openapi.internal.util.Utils;
import io.github.potjerodekool.openapi.log.Logger;
import io.github.potjerodekool.openapi.tree.OpenApi;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class SpringJacksonConfigGenerator extends AbstractSpringConfigGenerator {

    private static final Logger LOGGER = Logger.getLogger(SpringJacksonConfigGenerator.class.getName());

    private final Set<String> resolvedJaxsonModuleClasses;

    public SpringJacksonConfigGenerator(final OpenApiGeneratorConfig config,
                                        final TypeUtils typeUtils,
                                        final Filer filer,
                                        final DependencyChecker dependencyChecker) {
        super(config, typeUtils, filer);
        this.resolvedJaxsonModuleClasses = resolveDependencies(dependencyChecker);
    }

    private static Set<String> resolveDependencies(final DependencyChecker dependencyChecker) {
        return dependencyChecker.getProjectArtifacts().
                flatMap(artifact -> processArtifact(artifact).stream())
                .collect(Collectors.toSet());
    }

    private static Set<String> processArtifact(final Artifact artifact) {
        final Set<String> modules = new HashSet<>();
        final var file = artifact.file();

        if (file != null && file.isFile() && file.getName().toLowerCase().endsWith(".jar")) {
            try (final var jarFile = new JarFile(file)) {
                final var serviceFile = jarFile.getJarEntry("META-INF/services/com.fasterxml.jackson.databind.Module");

                if (serviceFile != null) {
                    LOGGER.info(String.format("Found jaxson databind modules in %s", file.getAbsolutePath()));
                    final var inputStream = jarFile.getInputStream(serviceFile);
                    final var moduleClassNames = new String(inputStream.readAllBytes()).split("\n");
                    modules.addAll(Arrays.asList(moduleClassNames));
                }
            } catch (final IOException e) {
                //Ignore exception
            }
        }

        return modules;
    }

    @Override
    protected String getConfigClassName() {
        return "JacksonConfiguration";
    }

    @Override
    protected void fillClass(final OpenApi api,
                             final TypeElement typeElement) {
        this.resolvedJaxsonModuleClasses.forEach(jaxsonModuleClassName -> addBeanMethod(typeElement, jaxsonModuleClassName));
    }

    @Override
    protected boolean skipGeneration() {
        return this.resolvedJaxsonModuleClasses.isEmpty();
    }

    private void addBeanMethod(final TypeElement typeElement,
                               final String moduleClassName) {
        final var parameterNamesModuleType = getTypeUtils().createDeclaredType(moduleClassName);

        final var sepIndex = moduleClassName.lastIndexOf(".");
        final String simpleName = sepIndex < 0 ? moduleClassName : moduleClassName.substring(sepIndex + 1);
        final var methodName = Utils.firstLower(simpleName);

        final var method = typeElement.addMethod(methodName, Modifier.PUBLIC);
        method.addAnnotation("org.springframework.context.annotation.Bean");
        method.addAnnotation(
                "org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean",
                new ArrayInitializerExpression(
                        LiteralExpression.createClassLiteralExpression(parameterNamesModuleType)
                )
        );
        method.setReturnType(parameterNamesModuleType);
        method.setBody(new BlockStatement(new ReturnStatement(new NewClassExpression(parameterNamesModuleType))));
    }
}