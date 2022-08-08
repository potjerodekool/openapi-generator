package io.github.potjerodekool.openapi.generate.config;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.expr.ClassExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.stmt.ReturnStmt;
import io.github.potjerodekool.openapi.DependencyChecker;
import io.github.potjerodekool.openapi.Filer;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.generate.Types;
import io.github.potjerodekool.openapi.tree.OpenApi;

import java.util.List;

public class SpringJacksonConfigGenerator extends AbstractSpringConfigGenerator {

    private final DependencyChecker dependencyChecker;

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

    public SpringJacksonConfigGenerator(final OpenApiGeneratorConfig config,
                                        final Types types,
                                        final Filer filer,
                                        final DependencyChecker dependencyChecker) {
        super(config, types, filer);
        this.dependencyChecker = dependencyChecker;
    }

    @Override
    protected String getConfigClassName() {
        return "JacksonConfiguration";
    }

    @Override
    protected void fillClass(final OpenApi api,
                             final ClassOrInterfaceDeclaration clazz) {
        dependencies.stream()
                .filter(jaxsonDependency -> dependencyChecker.isDependencyPresent(
                            jaxsonDependency.groupId(),
                            jaxsonDependency.artifactId()
                        )
                 )
                .forEach(jaxsonDependency ->
                        addBeanMethod(clazz, jaxsonDependency.moduleClassName())
                );
    }

    @Override
    protected boolean skipGeneration() {
        return dependencies.stream()
                .noneMatch(jaxsonDependency -> dependencyChecker.isDependencyPresent(
                        jaxsonDependency.groupId(),
                        jaxsonDependency.artifactId()
                ));
    }

    private void addBeanMethod(final ClassOrInterfaceDeclaration clazz,
                               final String moduleClassName) {
        final var parameterNamesModuleType = getTypes().createType(moduleClassName);

        final var method = clazz.addMethod("parameterNames");
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

record JaxsonDependency(String groupId,
                        String artifactId,
                        String moduleClassName) {

}