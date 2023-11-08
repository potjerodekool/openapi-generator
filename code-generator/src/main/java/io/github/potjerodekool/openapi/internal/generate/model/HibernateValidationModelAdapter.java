package io.github.potjerodekool.openapi.internal.generate.model;

import static io.github.potjerodekool.codegen.model.element.Name.getQualifiedNameOf;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
import io.github.potjerodekool.codegen.model.type.DeclaredType;
import io.github.potjerodekool.codegen.model.type.TypeMirror;
import io.github.potjerodekool.codegen.model.util.type.Types;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.internal.di.Bean;
import io.github.potjerodekool.openapi.internal.di.ConditionalOnDependency;
import io.github.potjerodekool.openapi.tree.media.OpenApiArraySchema;
import io.github.potjerodekool.openapi.tree.media.OpenApiSchema;
import jakarta.inject.Inject;

@Bean
@ConditionalOnDependency(
        groupId = "org.hibernate.validator",
        artifactId = "hibernate-validator"
)
public class HibernateValidationModelAdapter extends ValidationModelAdapter {

    private final TypeMirror numberType;

    @SuppressWarnings("initialization.fields.uninitialized")
    private final TypeTest futureTypeTest;
    private final Types types;

    @Inject
    public HibernateValidationModelAdapter(final GeneratorConfig generatorConfig,
                                           final Environment environment,
                                           final TypeTestLoader typeTestLoader) {
        super(generatorConfig, environment);
        final var elements = environment.getElementUtils();
        this.types = environment.getTypes();
        this.numberType = types.getDeclaredType(elements.getTypeElement("java.lang.Number"));
        this.futureTypeTest = typeTestLoader.loadTypeTest("validator.hv.future");
    }

    @Override
    public void adaptField(final VariableDeclaration<?> field) {
        super.adaptField(field);
        processUniqueItems(field.getMetaData(OpenApiSchema.class.getSimpleName()), field);
   }

    private void processUniqueItems(final OpenApiSchema<?> schema,
                                    final VariableDeclaration<?> field) {
        if (Boolean.TRUE.equals(schema.uniqueItems())
                && schema instanceof OpenApiArraySchema) {
            field.annotation(new AnnotationExpression("org.hibernate.validator.constraints.UniqueElements"));
        }
    }

    @Override
    protected boolean supportsDigits(final TypeMirror type) {
        if (types.isAssignable(numberType, type) ||
                isMonetaryAmount(type)) {
            return true;
        }

        return super.supportsDigits(type);
    }

    private boolean isMonetaryAmount(final TypeMirror type) {
        if (type instanceof DeclaredType declaredType) {
            return "javax.money.MonetaryAmount".equals(getQualifiedNameOf(declaredType.asElement()).toString());
        }

        return false;
    }

    @Override
    protected boolean isFutureSupported(final TypeMirror type) {
        return super.isFutureSupported(type)
                || futureTypeTest.test(type);
    }
}
