package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.loader.TypeElementLoader;
import io.github.potjerodekool.codegen.loader.asm.ClassPath;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.symbol.VariableSymbol;
import io.github.potjerodekool.codegen.model.tree.expression.NameExpression;
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ParameterizedType;
import io.github.potjerodekool.codegen.model.tree.type.TypeExpression;
import io.github.potjerodekool.codegen.model.type.DeclaredType;
import io.github.potjerodekool.codegen.model.util.Elements;
import io.github.potjerodekool.codegen.model.util.type.JavaTypes;
import io.github.potjerodekool.codegen.model.util.type.Types;
import io.github.potjerodekool.openapi.Features;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.internal.util.TypeUtils;
import io.github.potjerodekool.openapi.tree.Constraints;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;
import io.github.potjerodekool.openapi.tree.Package;
import io.github.potjerodekool.openapi.type.OpenApiObjectType;
import io.github.potjerodekool.openapi.type.OpenApiStandardType;
import io.github.potjerodekool.openapi.type.OpenApiStandardTypeEnum;
import org.checkerframework.com.google.errorprone.annotations.Var;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.lang.model.element.Element;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Disabled
@ExtendWith(MockitoExtension.class)
class HibernateValidationModelAdapterTest {

    private final GeneratorConfig generatorConfig = new GeneratorConfig(
            Language.JAVA,
            "",
            Map.of(
                    Features.FEATURE_JAKARTA, true
            )
    );

    private final Environment environment = new Environment(ClassPath.getJavaClassPath());
    private final Elements elements = environment.getElementUtils();
    private final Types types = environment.getTypes();
    private final TypeUtils typeUtils = new TypeUtils(types, elements);

    private final HibernateValidationModelAdapter hibernateValidationModelAdapter = new HibernateValidationModelAdapter(
            generatorConfig,
            environment,
            typeUtils
    );

    private final Map<String, String>typeMappings = Map.of(
            "integer", "java.lang.Integer",
            "string", "java.lang.String"
    );

    private OpenApiObjectType createOpenApiObjectType(final String className) {
        final var separatorIndex = className.lastIndexOf('.');
        final Package packageObj;
        final String simpleName;

        if (separatorIndex < 0) {
            packageObj = Package.UNNAMED;
            simpleName = className;
        } else {
            packageObj = new Package(className.substring(0, separatorIndex));
            simpleName = className.substring(separatorIndex + 1);
        }

        return new OpenApiObjectType(
                packageObj,
                simpleName,
                Map.of(),
                null
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {"integer", "string"})
    void nullConstraint(final String type) {
        final var extensions = new HashMap<String, Object>();
        extensions.put(Constraints.X_ASSERT, null);

        final var property = new OpenApiProperty(
                new OpenApiStandardType(
                        OpenApiStandardTypeEnum.fromType(type),
                        null,
                        true
                ),
                false,
                false,
                false,
                null,
                new Constraints().extensions(extensions)
        );

        final var fieldType = new ParameterizedType(
                new NameExpression(
                        typeMappings.get(type)
                )
        );

        final var field = new VariableDeclaration(
                ElementKind.FIELD,
                Set.of(), fieldType,
                "name",
                null,
                null
        );

        hibernateValidationModelAdapter.adaptField(property, field);

        assertTrue(field.isAnnotationPresent(jakarta.validation.constraints.Null.class.getName()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"integer", "string"})
    void notNullConstraint(final String type) {
        final var property = new OpenApiProperty(
                new OpenApiStandardType(
                        OpenApiStandardTypeEnum.fromType(type),
                        null,
                        false
                ),
                false,
                false,
                false,
                null,
                new Constraints()
        );

        final var fieldType = new ParameterizedType(
                new NameExpression(typeMappings.get(type))
        );

        final var field = new VariableDeclaration(
                ElementKind.FIELD,
                Set.of(),
                fieldType,
                "name",
                null,
                null
        );

        hibernateValidationModelAdapter.adaptField(property, field);

        assertTrue(field.isAnnotationPresent(jakarta.validation.constraints.NotNull.class.getName()));
    }

    @Test
    void assertTrueConstraint() {
        final var property = new OpenApiProperty(
                new OpenApiStandardType(
                        OpenApiStandardTypeEnum.BOOLEAN,
                        null,
                        false
                ),
                false,
                false,
                false,
                null,
                new Constraints().extensions(Map.of("x-assert", true))
        );

        final var fieldType = new ParameterizedType(new NameExpression("java.lang.Boolean"));

        final var field = new VariableDeclaration(
                ElementKind.FIELD,
                Set.of(),
                fieldType,
                "enabled",
                null,
                null
        );

        hibernateValidationModelAdapter.adaptField(property, field);

        assertTrue(field.isAnnotationPresent(jakarta.validation.constraints.AssertTrue.class.getName()));
    }

    @Test
    void assertFalseConstraint() {
        final var property = new OpenApiProperty(
                new OpenApiStandardType(
                        OpenApiStandardTypeEnum.BOOLEAN,
                        null,
                        false
                ),
                false,
                false,
                false,
                null,
                new Constraints().extensions(Map.of("x-assert", false))
        );

        final var fieldType = new ParameterizedType(
                new NameExpression("java.lang.Boolean")
        );

        final var field = new VariableDeclaration(
                ElementKind.FIELD,
                Set.of(),
                fieldType,
                "enabled",
                null,
                null
        );

        hibernateValidationModelAdapter.adaptField(property, field);

        assertTrue(field.isAnnotationPresent(jakarta.validation.constraints.AssertFalse.class.getName()));
    }

    @Test
    void minConstraint() {
        final var property = new OpenApiProperty(
                new OpenApiStandardType(
                        OpenApiStandardTypeEnum.INTEGER,
                        null,
                        false
                ),
                false,
                false,
                false,
                null,
                new Constraints().minimum(18)
        );

        final var fieldType = new ParameterizedType(
                new NameExpression("java.lang.Integer")
        );

        final var field = new VariableDeclaration(
                ElementKind.FIELD,
                Set.of(),
                fieldType,
                "minAge",
                null,
                null
        );

        hibernateValidationModelAdapter.adaptField(property, field);

        assertTrue(field.isAnnotationPresent(jakarta.validation.constraints.Min.class.getName()));
    }

    @Test
    void maxConstraint() {
        final var property = new OpenApiProperty(
                new OpenApiStandardType(
                        OpenApiStandardTypeEnum.INTEGER,
                        null,
                        false
                ),
                false,
                false,
                false,
                null,
                new Constraints().maximum(65)
        );

        final var fieldType = new ParameterizedType(
                new NameExpression("java.lang.Integer")
        );


        final var field = new VariableDeclaration(
                ElementKind.FIELD,
                Set.of(),
                fieldType,
                "pensionAge",
                null,
                null
        );

        hibernateValidationModelAdapter.adaptField(property, field);

        assertTrue(field.isAnnotationPresent(jakarta.validation.constraints.Max.class.getName()));
    }

    @Test
    void decimalMinConstraint() {
        final var property = new OpenApiProperty(
                new OpenApiStandardType(
                        OpenApiStandardTypeEnum.NUMBER,
                        null,
                        false
                ),
                false,
                false,
                false,
                null,
                new Constraints().minimum(18.5)
        );

        final var fieldType = new ParameterizedType(
                new NameExpression("java.math.BigDecimal")
        );

        final var field = new VariableDeclaration(
                ElementKind.FIELD,
                Set.of(),
                fieldType,
                "minAge",
                null,
                null
        );

        hibernateValidationModelAdapter.adaptField(property, field);

        assertTrue(field.isAnnotationPresent(jakarta.validation.constraints.DecimalMin.class.getName()));
    }

    @Test
    void decimalMaxConstraint() {
        final var property = new OpenApiProperty(
                new OpenApiStandardType(
                        OpenApiStandardTypeEnum.NUMBER,
                        null,
                        false
                ),
                false,
                false,
                false,
                null,
                new Constraints().maximum(65.5)
        );

        final var fieldType = new ParameterizedType(
                new NameExpression("java.math.BigInteger")
        );

        final var field = new VariableDeclaration(
                ElementKind.FIELD,
                Set.of(),
                fieldType,
                "pensionAge",
                null,
                null
        );

        hibernateValidationModelAdapter.adaptField(property, field);

        assertTrue(field.isAnnotationPresent(jakarta.validation.constraints.DecimalMax.class.getName()));
    }

    @Test
    void negativeConstraint() {
        final var property = new OpenApiProperty(
                new OpenApiStandardType(
                        OpenApiStandardTypeEnum.NUMBER,
                        null,
                        false
                ),
                false,
                false,
                false,
                null,
                new Constraints().extensions(Map.of("x-assert", "negative"))
        );

        final var fieldType = new ParameterizedType(
                new NameExpression("java.math.BigInteger")
        );

        final var field = new VariableDeclaration(
                ElementKind.FIELD,
                Set.of(),
                fieldType,
                "pensionAge",
                null,
                null
        );

        hibernateValidationModelAdapter.adaptField(property, field);

        assertTrue(field.isAnnotationPresent(jakarta.validation.constraints.Negative.class.getName()));
    }

    @Test
    void negativeOrzeroConstraint() {
        final var property = new OpenApiProperty(
                new OpenApiStandardType(
                        OpenApiStandardTypeEnum.NUMBER,
                        null,
                        false
                ),
                false,
                false,
                false,
                null,
                new Constraints().extensions(Map.of("x-assert", "negativeOrZero"))
        );

        final var fieldType = new ParameterizedType(
                new NameExpression("java.math.BigInteger")
        );

        final var field = new VariableDeclaration(
                ElementKind.FIELD,
                Set.of(),
                fieldType,
                "pensionAge",
                null,
                null
        );

        hibernateValidationModelAdapter.adaptField(property, field);

        assertTrue(field.isAnnotationPresent(jakarta.validation.constraints.NegativeOrZero.class.getName()));
    }

    @Test
    void positiveConstraint() {
        final var property = new OpenApiProperty(
                new OpenApiStandardType(
                        OpenApiStandardTypeEnum.NUMBER,
                        null,
                        false
                ),
                false,
                false,
                false,
                null,
                new Constraints().extensions(Map.of("x-assert", "positive"))
        );

        final var fieldType = new ParameterizedType(
                new NameExpression("java.math.BigInteger")
        );

        final var field = new VariableDeclaration(
                ElementKind.FIELD,
                Set.of(),
                fieldType,
                "pensionAge",
                null,
                null
        );

        hibernateValidationModelAdapter.adaptField(property, field);

        assertTrue(field.isAnnotationPresent(jakarta.validation.constraints.Positive.class.getName()));
    }

    @Test
    void positiveOrZeroConstraint() {
        final var property = new OpenApiProperty(
                new OpenApiStandardType(
                        OpenApiStandardTypeEnum.NUMBER,
                        null,
                        false
                ),
                false,
                false,
                false,
                null,
                new Constraints().extensions(Map.of("x-assert", "positiveOrZero"))
        );

        final var fieldType = new ParameterizedType(
                new NameExpression("java.math.BigInteger")
        );

        final var field = new VariableDeclaration(
                ElementKind.FIELD,
                Set.of(),
                fieldType,
                "pensionAge",
                null,
                null
        );

        hibernateValidationModelAdapter.adaptField(property, field);

        assertTrue(field.isAnnotationPresent(jakarta.validation.constraints.PositiveOrZero.class.getName()));
    }

    @ParameterizedTest()
    @ValueSource(strings = {"java.lang.String", "java.util.List", "java.util.Map"})
    void sizeConstraint(final String className) {
        final var constraints = new Constraints();

        if ("java.lang.String".equals(className)) {
            constraints.minLength(1).maxLength(10);
        } else {
            constraints.minItems(1).maxItems(10);
        }

        final var property = new OpenApiProperty(
                createOpenApiObjectType(className),
                false,
                false,
                false,
                null,
                constraints
        );

        final TypeExpression fieldType = new ParameterizedType(
                new NameExpression(className)
        );

        final var field = new VariableDeclaration(
                ElementKind.FIELD,
                Set.of(),
                fieldType,
                "it",
                null,
                null
        );

        hibernateValidationModelAdapter.adaptField(property, field);

        assertTrue(field.isAnnotationPresent(jakarta.validation.constraints.Size.class.getName()));
    }

}