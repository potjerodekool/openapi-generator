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
import io.github.potjerodekool.openapi.internal.util.TypeUtils;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;
import jakarta.inject.Inject;

@Bean
@ConditionalOnDependency(
        groupId = "org.hibernate.validator",
        artifactId = "hibernate-validator"
)
public class HibernateValidationModelAdapter extends ValidationModelAdapter {

    private final TypeMirror numberType;

    @SuppressWarnings("initialization.fields.uninitialized")
    private TypeTest futureTypeTest;
    private final Types types;

    @Inject
    public HibernateValidationModelAdapter(final GeneratorConfig generatorConfig,
                                           final Environment environment,
                                           final TypeUtils typeUtils) {
        super(generatorConfig, environment, typeUtils);
        final var elements = environment.getElementUtils();
        this.types = environment.getTypes();
        this.numberType = types.getDeclaredType(elements.getTypeElement("java.lang.Number"));
    }

    @Inject
    public void initTypes(final TypeTestLoader typeTestLoader) {
        this.futureTypeTest = typeTestLoader.loadTypeTest("validator.hv.future");
    }

    @Override
    public void adaptField(final OpenApiProperty property,
                           final VariableDeclaration field) {
        super.adaptField(property, field);
        processUniqueItems(property, field);
   }

    private void processUniqueItems(final OpenApiProperty property,
                                    final VariableDeclaration field) {
        final var fieldType = field.getVarType().getType();

        if (Boolean.TRUE.equals(property.constraints().uniqueItems()) && typeUtils.isListType(fieldType)) {
            field.addAnnotation(new AnnotationExpression("org.hibernate.validator.constraints.UniqueElements"));
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
