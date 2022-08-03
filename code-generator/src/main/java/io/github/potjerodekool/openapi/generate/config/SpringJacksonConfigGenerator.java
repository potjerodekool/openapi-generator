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

public class SpringJacksonConfigGenerator extends AbstractSpringConfigGenerator {

    private final DependencyChecker dependencyChecker;

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

        if (dependencyChecker.isDependencyPresent(
                "com.fasterxml.jackson.module",
                "jackson-module-parameter-names")) {
            addBeanMethod(clazz, "com.fasterxml.jackson.module.paramnames.ParameterNamesModule");
        }

        if (dependencyChecker.isDependencyPresent(
                "com.fasterxml.jackson.datatype",
                "jackson-datatype-jsr310")) {
            addBeanMethod(clazz, "com.fasterxml.jackson.datatype.jsr310.JavaTimeModule");
        }

        if (dependencyChecker.isDependencyPresent(
                "com.fasterxml.jackson.datatype",
                "jackson-datatype-jdk8")) {
            addBeanMethod(clazz, "com.fasterxml.jackson.datatype.jdk8.Jdk8Module");
        }
    }

    private void addBeanMethod(final ClassOrInterfaceDeclaration clazz,
                               final String moduleClassName) {
        final var parameterNamesModuleType = types.createType(moduleClassName);

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
