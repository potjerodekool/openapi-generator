package io.github.potjerodekool.openapi.internal.generate.model;

import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.Type;
import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.RequestCycleLocation;
import io.github.potjerodekool.openapi.internal.generate.GenerateUtils;
import io.github.potjerodekool.openapi.internal.generate.GenerateUtilsJava;
import io.github.potjerodekool.openapi.internal.generate.Types;
import io.github.potjerodekool.openapi.internal.generate.TypesJava;
import io.github.potjerodekool.openapi.tree.Constraints;
import io.github.potjerodekool.openapi.tree.Digits;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;
import io.github.potjerodekool.openapi.tree.Package;
import io.github.potjerodekool.openapi.type.OpenApiObjectType;
import io.github.potjerodekool.openapi.type.OpenApiStandardType;
import io.github.potjerodekool.openapi.type.OpenApiStandardTypeEnum;

import io.github.potjerodekool.openapi.type.OpenApiType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.javaparser.ast.Modifier.createModifierList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class ValidationModelAdapterTest {

    private final Types types = new TypesJava();

    private final GenerateUtils generateUtils = new GenerateUtilsJava(types);

    private ValidationModelAdapter validationModelAdapter = null;

    @BeforeEach
    void setup() {
        final var config = mock(OpenApiGeneratorConfig.class);

        if (validationModelAdapter == null) {
            validationModelAdapter = new ValidationModelAdapter(
                    config,
                    types,
                    generateUtils
            );
        }
    }

    private FieldDeclaration createField(final Type type,
                                         final String name) {
        final var fieldDeclaration = new FieldDeclaration();
        final var variable = new VariableDeclarator(type, name);
        fieldDeclaration.getVariables().add(variable);
        fieldDeclaration.setModifiers(createModifierList(Modifier.Keyword.PRIVATE));
        fieldDeclaration.setParentNode(types.createCompilationUnit());
        return fieldDeclaration;
    }

    private PropertyAndField createPropertyAndField(final OpenApiType type,
                                                    final Constraints constraints) {

        final var property = new OpenApiProperty(
                type,
                false,
                null,
                null,
                constraints
        );

        final var fieldDeclaration = createField(
                types.createType(type),
                "field"
        );

        return new PropertyAndField(property, fieldDeclaration);
    }

    @Test
    void assertFalseAnnotation() {
        final var type = new OpenApiStandardType(
                OpenApiStandardTypeEnum.BOOLEAN,
                null,
                false
        );

        final var constrains = new Constraints();
        constrains.allowedValue(false);

        final var propertyAndField = createPropertyAndField(type,constrains);
        final var property = propertyAndField.property();
        final var fieldDeclaration = propertyAndField.fieldDeclaration();

        validationModelAdapter.adaptField(
                HttpMethod.POST,
                RequestCycleLocation.REQUEST,
                property,
                fieldDeclaration
        );

        assertTrue(getAnnotation(fieldDeclaration, "javax.validation.constraints.AssertFalse").isPresent());
    }

    @Test
    void assertTrueAnnotation() {
        final var type = new OpenApiStandardType(
                OpenApiStandardTypeEnum.BOOLEAN,
                null,
                false
        );

        final var constrains = new Constraints();
        constrains.allowedValue(true);

        final var propertyAndField = createPropertyAndField(type,constrains);
        final var property = propertyAndField.property();
        final var fieldDeclaration = propertyAndField.fieldDeclaration();

        validationModelAdapter.adaptField(
                HttpMethod.POST,
                RequestCycleLocation.REQUEST,
                property,
                fieldDeclaration
        );

        assertTrue(getAnnotation(fieldDeclaration, "javax.validation.constraints.AssertTrue").isPresent());
    }

    @Test
    void digits() {
        final var types = List.of(
                Pair.of("java.math.BigDecimal", new Digits(3,2)),
                Pair.of("java.math.BigInteger", new Digits(5, 0)),
                Pair.of("java.lang.String", new Digits(0, 0)),
                Pair.of("byte", new Digits(0, 0)),
                Pair.of("java.lang.Byte", new Digits(0, 0)),
                Pair.of("short", new Digits(2, 0)),
                Pair.of("java.lang.Short", new Digits(3, 0)),
                Pair.of("int", new Digits(10, 0)),
                Pair.of("java.lang.Integer", new Digits(0, 0)),
                Pair.of("long", new Digits(8, 0)),
                Pair.of("java.lang.Long", new Digits(8, 0))
        );

        types.stream()
                .map( (typeAndDigits) -> Pair.of(createType(typeAndDigits.first()), typeAndDigits.second()))
                .forEach(typeAndDigits -> {
                    final var constrains = new Constraints();
                    constrains.digits(typeAndDigits.second());

                    final var propertyAndField = createPropertyAndField(typeAndDigits.first(),constrains);
                    final var property = propertyAndField.property();
                    final var fieldDeclaration = propertyAndField.fieldDeclaration();

                    validationModelAdapter.adaptField(
                            HttpMethod.POST,
                            RequestCycleLocation.REQUEST,
                            property,
                            fieldDeclaration
                    );

                    final var annotationOptional =
                            getAnnotation(fieldDeclaration, "javax.validation.constraints.Digits");
                    assertTrue(annotationOptional.isPresent());
                    final var annotation = (NormalAnnotationExpr) annotationOptional.get();

                    final var integer = getMemberValuePair(annotation.getPairs(), "integer");
                    assertLiteralValue(typeAndDigits.second().integer(), integer.getValue());

                    final var fraction = getMemberValuePair(annotation.getPairs(), "fraction");
                    assertLiteralValue(typeAndDigits.second().fraction(), fraction.getValue());
                });
    }

    private MemberValuePair getMemberValuePair(final NodeList<MemberValuePair> pairs, final String name) {
        final var optional = pairs.stream().filter(it -> it.getName().asString().equals(name))
                .findFirst();
        assertTrue(optional.isPresent(), String.format("membervaluepair %s is absent", name));
        return optional.get();
    }

    private void assertLiteralValue(final Object value,
                                    final Expression expression) {
        assertTrue(expression instanceof LiteralExpr, String.format("expression is not a litteral but %s",
                expression.getClass().getName()));
        if (expression.isIntegerLiteralExpr()) {
            final var integerLiteralExpression = (IntegerLiteralExpr) expression;

            assertEquals(value,
                    integerLiteralExpression.asNumber(),
                    String.format("%s is not %s", value, integerLiteralExpression.asNumber()))
            ;
        } else {
            fail("Unhandled " + expression.getClass().getName());
        }
    }

    private Optional<AnnotationExpr> getAnnotation(final FieldDeclaration fieldDeclaration,
                                                   final String className) {
        return fieldDeclaration.getAnnotations().stream()
                .filter(a -> a.getName().asString().equals(className))
                .findFirst();
    }

    private OpenApiType createType(final String name) {
        return switch (name) {
            case "byte" -> new OpenApiStandardType(OpenApiStandardTypeEnum.BYTE, null, false);
            case "short" -> new OpenApiStandardType(OpenApiStandardTypeEnum.SHORT, null, false);
            case "int" -> new OpenApiStandardType(OpenApiStandardTypeEnum.INTEGER, "int32", false);
            case "long" -> new OpenApiStandardType(OpenApiStandardTypeEnum.INTEGER, "int64", false);
            default -> {
                final var sepIndex = name.lastIndexOf('.');
                final Package pck;
                final String simpleName;

                if (sepIndex > -1) {
                    pck = new Package(name.substring(0, sepIndex));
                    simpleName = name.substring(sepIndex + 1);
                } else {
                    pck = Package.UNNAMED;
                    simpleName = name;
                }
                yield new OpenApiObjectType(
                        pck,
                        simpleName,
                        Map.of(),
                        null
                );
            }
        };
    }

}

record PropertyAndField(OpenApiProperty property,
                        FieldDeclaration fieldDeclaration) {

}

record Pair<A,B>(A first, B second) {

    static <A,B> Pair<A,B> of(A first, B second) {
        return new Pair<>(first, second);
    }
}