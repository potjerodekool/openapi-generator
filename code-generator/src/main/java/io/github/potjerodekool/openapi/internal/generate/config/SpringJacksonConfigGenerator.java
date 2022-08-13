package io.github.potjerodekool.openapi.internal.generate.config;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import io.github.potjerodekool.openapi.*;
import io.github.potjerodekool.openapi.internal.*;
import io.github.potjerodekool.openapi.internal.generate.Types;
import io.github.potjerodekool.openapi.tree.OpenApi;
import io.github.potjerodekool.openapi.internal.util.Utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

public class SpringJacksonConfigGenerator extends AbstractSpringConfigGenerator {

    private static final Logger LOGGER = Logger.getLogger(SpringJacksonConfigGenerator.class.getName());

    private final Set<String> resolvedJaxsonModuleClasses;

    /*
    private final List<JaxsonDependency> dependencies = List.of(
            new JaxsonDependency(
                    "com.fasterxml.jackson.module",
                    "jackson-module-parameter-names",
                    "com.fasterxml.jackson.module.paramnames.ParameterNamesModule"
            ),
            new JaxsonDependency(
                    "com.fasterxml.jackson.datatype",
                    "jackson-datatype-jsr310",
                    "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule"
            ),
            new JaxsonDependency(
                    "com.fasterxml.jackson.datatype",
                    "jackson-datatype-jdk8",
                    "com.fasterxml.jackson.datatype.jdk8.Jdk8Module"
            )
    );
     */

    public SpringJacksonConfigGenerator(final OpenApiGeneratorConfig config,
                                        final Types types,
                                        final Filer filer,
                                        final DependencyChecker dependencyChecker) {
        super(config, types, filer);
        this.resolvedJaxsonModuleClasses = resolveDependencies(dependencyChecker);
    }

    private static Set<String> resolveDependencies(final DependencyChecker dependencyChecker) {
        return dependencyChecker.getProjectArtifacts().
                flatMap(artifact -> processArtifact(artifact).stream())
                .collect(Collectors.toSet());
    }

    //TODO Scan dependencies
    private static Set<String> processArtifact(final Artifact artifact) {
        final Set<String> modules = new HashSet<>();
        final var file = artifact.file();

        if (file.isFile() && file.getName().toLowerCase().endsWith(".jar")) {
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
                             final ClassOrInterfaceDeclaration clazz) {
        this.resolvedJaxsonModuleClasses.forEach(jaxsonModuleClassName -> addBeanMethod(clazz, jaxsonModuleClassName));

        /*
        dependencies.stream()
                .filter(jaxsonDependency -> dependencyChecker.isDependencyPresent(
                            jaxsonDependency.groupId(),
                            jaxsonDependency.artifactId()
                        )
                 )
                .forEach(jaxsonDependency ->
                        addBeanMethod(clazz, jaxsonDependency.moduleClassName())
                );
         */
    }

    @Override
    protected boolean skipGeneration() {
        return this.resolvedJaxsonModuleClasses.isEmpty();
    }



    private void addBeanMethod(final ClassOrInterfaceDeclaration clazz,
                               final String moduleClassName) {
        final var parameterNamesModuleType = getTypes().createType(moduleClassName);

        final var sepIndex = moduleClassName.lastIndexOf(".");
        final String simpleName = sepIndex < 0 ? moduleClassName : moduleClassName.substring(sepIndex + 1);
        final var methodName = Utils.firstLower(simpleName);

        final var method = clazz.addMethod(methodName, Modifier.Keyword.PUBLIC);
        method.addMarkerAnnotation("org.springframework.context.annotation.Bean");
        method.addSingleMemberAnnotation(
                "org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean",
                new ClassExpr(parameterNamesModuleType)
        );
        method.setType(parameterNamesModuleType);

        method.setBody(new BlockStmt(
                NodeList.nodeList(new ReturnStmt(new ObjectCreationExpr()
                        .setType(parameterNamesModuleType)))
        ));
    }
}

/*
record JaxsonDependency(String groupId,
                        String artifactId,
                        String moduleClassName) {

}
*/