package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.RequestCycleLocation;
import io.github.potjerodekool.openapi.internal.ast.TypeUtils;
import io.github.potjerodekool.openapi.internal.ast.element.VariableElement;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import io.github.potjerodekool.openapi.internal.di.Bean;
import io.github.potjerodekool.openapi.internal.di.ConditionalOnDependency;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;
import jakarta.inject.Inject;

@Bean
@ConditionalOnDependency(
        groupId = "org.hibernate.validator",
        artifactId = "hibernate-validator"
)
public class HibernateValidationModelAdapter extends ValidationModelAdapter {

    private final Type<?> numberType;

    @SuppressWarnings("initialization.fields.uninitialized")
    private TypeTest futureTypeTest;

    @Inject
    public HibernateValidationModelAdapter(final OpenApiGeneratorConfig config,
                                           final TypeUtils typeUtils) {
        super(config, typeUtils);
        this.numberType = typeUtils.createDeclaredType("java.lang.Number");
    }

    @Inject
    public void initTypes(final TypeTestLoader typeTestLoader) {
        this.futureTypeTest = typeTestLoader.loadTypeTest("validator.hv.future");
    }

    @Override
    public void adaptField(final HttpMethod httpMethod,
                           final RequestCycleLocation requestCycleLocation,
                           final OpenApiProperty property,
                           final VariableElement field) {
        super.adaptField(httpMethod, requestCycleLocation, property, field);
        processUniqueItems(property, field);
   }

    private void processUniqueItems(final OpenApiProperty property,
                                    final VariableElement field) {
        final var fieldType = field.getType();

        if (Boolean.TRUE.equals(property.constraints().uniqueItems()) && getTypeUtils().isListType(fieldType)) {
            field.addAnnotation("org.hibernate.validator.constraints.UniqueElements");
        }
    }

    @Override
    protected boolean supportsDigits(final Type<?> type) {
        if (numberType.isAssignableBy(type) ||
                "javax.money.MonetaryAmount".equals(getTypeUtils().getTypeName(type))) {
            return true;
        }

        return super.supportsDigits(type);
    }

    @Override
    protected boolean isFutureSupported(final Type<?> type) {
        return super.isFutureSupported(type)
                || futureTypeTest.test(type);
    }
}
