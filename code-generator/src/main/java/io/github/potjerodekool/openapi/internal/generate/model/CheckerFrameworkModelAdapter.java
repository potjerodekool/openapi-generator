package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.model.Attribute;
import io.github.potjerodekool.codegen.model.symbol.ClassSymbol;
import io.github.potjerodekool.codegen.model.symbol.MethodSymbol;
import io.github.potjerodekool.codegen.model.tree.MethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.expression.Expression;
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
import io.github.potjerodekool.codegen.model.type.AbstractType;
import io.github.potjerodekool.codegen.model.type.DeclaredType;
import io.github.potjerodekool.codegen.model.type.TypeMirror;
import io.github.potjerodekool.codegen.model.util.Elements;
import io.github.potjerodekool.codegen.model.util.type.Types;
import io.github.potjerodekool.openapi.*;
import io.github.potjerodekool.openapi.generate.model.ModelAdapter;
import io.github.potjerodekool.openapi.internal.di.Bean;
import io.github.potjerodekool.openapi.internal.di.ConditionalOnDependency;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;
import jakarta.inject.Inject;

import java.util.Set;

@Bean
@ConditionalOnDependency(
        groupId = "org.checkerframework",
        artifactId = "checker-qual"
)
public class CheckerFrameworkModelAdapter implements ModelAdapter {

    private static final Set<String> CONTAINER_CLASSES = Set.of(
            "org.openapitools.jackson.nullable.JsonNullable",
            "java.util.List",
            "java.util.Set"
    );

    private final Types types;

    private final Elements elements;

    private final boolean isEnabled;

    @Inject
    public CheckerFrameworkModelAdapter(final GeneratorConfig generatorConfig,
                                        final Environment environment) {
        this.types = environment.getTypes();
        this.elements = environment.getElementUtils();
        this.isEnabled = generatorConfig.language() == Language.JAVA
                && generatorConfig.isFeatureEnabled(Features.FEATURE_CHECKER);
    }

    @Override
    public void adaptConstructor(final MethodDeclaration constructor) {
        if (!isEnabled) {
            return;
        }

        constructor.getParameters().forEach(parameter -> {
            final var newType = addAnnotationIfNeeded(parameter.getVarType());
            parameter.setVarType(newType);
        });
    }

    @Override
    public void adaptField(final OpenApiProperty property,
                           final VariableDeclaration fieldElement) {
        if (!isEnabled) {
            return;
        }

        final var newFieldType = addAnnotationIfNeeded(fieldElement.getVarType());
        fieldElement.setVarType(newFieldType);
    }

    @Override
    public void adaptGetter(final OpenApiProperty property,
                            final MethodDeclaration methodDeclaration) {
        if (!isEnabled) {
            return;
        }

        final var returnType = addAnnotationIfNeeded(methodDeclaration.getReturnType());
        methodDeclaration.setReturnType(returnType);
    }

    @Override
    public void adaptSetter(final OpenApiProperty property,
                            final MethodDeclaration method) {
        if (!isEnabled) {
            return;
        }

        final var parameter = method.getParameters().get(0);
        final var newParameterType = addAnnotationIfNeeded(parameter.getVarType());
        parameter.setVarType(newParameterType);
    }

    private Expression addAnnotationIfNeeded(final Expression type) {
        throw new UnsupportedOperationException();
        /*
        if (!isContainerType(type) && type.isNullable() && type instanceof DeclaredType dt) {
            final var newDeclaredType = (AbstractType) types.getDeclaredType(
                    (ClassSymbol) dt.asElement()
            );
            newDeclaredType.addAnnotation(Attribute.compound(
                    (ClassSymbol) elements.getTypeElement("org.checkerframework.checker.nullness.qual.Nullable"))
            );
            return newDeclaredType;
        } else {
            return type;
        }
         */
    }

    private boolean isContainerType(final TypeMirror type) {
        if (type.isArrayType()) {
            return true;
        } else {
            final var element = types.asElement(type);

            if (element != null){
                return CONTAINER_CLASSES.contains(Elements.getQualifiedName(element).toString());
            }
        }

        return false;
    }
}
