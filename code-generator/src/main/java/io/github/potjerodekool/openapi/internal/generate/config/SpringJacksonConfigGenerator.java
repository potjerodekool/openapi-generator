package io.github.potjerodekool.openapi.internal.generate.config;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.model.Attribute;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.symbol.ClassSymbol;
import io.github.potjerodekool.codegen.model.tree.expression.*;
import io.github.potjerodekool.codegen.model.tree.statement.BlockStatement;
import io.github.potjerodekool.codegen.model.tree.statement.ReturnStatement;
import io.github.potjerodekool.codegen.model.util.StringUtils;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.dependency.Artifact;
import io.github.potjerodekool.openapi.dependency.DependencyChecker;
import io.github.potjerodekool.openapi.log.Logger;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class SpringJacksonConfigGenerator extends AbstractSpringConfigGenerator {

    private static final Logger LOGGER = Logger.getLogger(SpringJacksonConfigGenerator.class.getName());

    private final Set<String> resolvedJaxsonModuleClasses;

    public SpringJacksonConfigGenerator(final GeneratorConfig generatorConfig,
                                        final Environment environment,
                                        final DependencyChecker dependencyChecker) {
        super(generatorConfig, environment);
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
    protected void fillClass(final ClassSymbol typeElement) {
        this.resolvedJaxsonModuleClasses.forEach(jaxsonModuleClassName -> {
            if ("com.fasterxml.jackson.module.kotlin.KotlinModule".equals(jaxsonModuleClassName)) {
                addKotlinModuleBeanMethod(typeElement, jaxsonModuleClassName);
            } else {
                addBeanMethod(typeElement, jaxsonModuleClassName);
            }
        });
    }

    @Override
    protected boolean skipGeneration() {
        return this.resolvedJaxsonModuleClasses.isEmpty();
    }

    private void addBeanMethod(final ClassSymbol typeElement,
                               final String moduleClassName) {
        final var parameterNamesModuleType = getTypes().getDeclaredType(
                getElementUtils().getTypeElement(moduleClassName)
        );

        final var sepIndex = moduleClassName.lastIndexOf(".");
        final String simpleName = sepIndex < 0 ? moduleClassName : moduleClassName.substring(sepIndex + 1);
        final var methodName = StringUtils.firstLower(simpleName);

        final var method = typeElement.addMethod(methodName, parameterNamesModuleType, Modifier.PUBLIC);
        method.addAnnotation(loadClass("org.springframework.context.annotation.Bean"));
        method.addAnnotation(
                loadClass("org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean"),
                Attribute.array(
                        Attribute.clazz(parameterNamesModuleType)
                )
        );
        method.setBody(new BlockStatement(new ReturnStatement(new NewClassExpression(parameterNamesModuleType))));
    }

    private void addKotlinModuleBeanMethod(final ClassSymbol typeElement,
                                           final String moduleClassName) {
        final var builderTypeElement = getElementUtils().getTypeElement(moduleClassName + ".Builder");

        if (builderTypeElement == null) {
            return;
        }

        final var parameterNamesModuleType = getTypes().getDeclaredType(
                getElementUtils().getTypeElement(moduleClassName));

        final var sepIndex = moduleClassName.lastIndexOf(".");
        final String simpleName = sepIndex < 0 ? moduleClassName : moduleClassName.substring(sepIndex + 1);
        final var methodName = StringUtils.firstLower(simpleName);

        final var method = typeElement.addMethod(methodName, parameterNamesModuleType, Modifier.PUBLIC);
        method.addAnnotation(loadClass("org.springframework.context.annotation.Bean"));
        method.addAnnotation(
                loadClass("org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean"),
                Attribute.array(
                        Attribute.clazz(parameterNamesModuleType)
                )
        );

        final var builderType = getTypes().getDeclaredType(
                builderTypeElement
        );

        final var buildCall = new MethodCallExpression(
                new MethodCallExpression(
                        new NewClassExpression(builderType),
                        "configure",
                        List.of(
                                new FieldAccessExpression(
                                        new NameExpression("com.fasterxml.jackson.module.kotlin.KotlinFeature"),
                                        "StrictNullChecks"
                                ),
                                LiteralExpression.createBooleanLiteralExpression(true)
                        )
                ),
                "build"
        );

        method.setBody(new BlockStatement(new ReturnStatement(buildCall)));
    }

    private ClassSymbol loadClass(final String className) {
        return (ClassSymbol) getElementUtils().getTypeElement(className);
    }
}