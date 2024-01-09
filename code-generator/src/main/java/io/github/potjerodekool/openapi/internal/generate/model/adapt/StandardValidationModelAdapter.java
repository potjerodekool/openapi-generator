package io.github.potjerodekool.openapi.internal.generate.model.adapt;

import io.github.potjerodekool.codegen.model.CompilationUnit;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.JTreeFilter;
import io.github.potjerodekool.codegen.model.tree.expression.LiteralExpression;
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.codegen.model.type.DeclaredType;
import io.github.potjerodekool.codegen.model.type.PrimitiveType;
import io.github.potjerodekool.codegen.model.type.TypeMirror;
import io.github.potjerodekool.openapi.internal.generate.model.ValidationExtensions;
import io.github.potjerodekool.openapi.internal.generate.model.Extensions;
import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.math.BigDecimal;
import java.util.Map;

import static io.github.potjerodekool.codegen.model.element.Name.getQualifiedNameOf;

public class StandardValidationModelAdapter implements ValidationModelAdapter {

    private final String constraintsPackage;

    public StandardValidationModelAdapter() {
        final var validationBasePackage = "jakarta" + ".validation";
        this.constraintsPackage = validationBasePackage + ".constraints";
    }

    @Override
    public void adapt(final HttpMethod method, final ObjectSchema schema, final CompilationUnit unit) {
        final var classDeclarations = unit.getClassDeclarations();

        if (classDeclarations.isEmpty()) {
            return;
        }

        final var classDeclaration = classDeclarations.getFirst();
        final var properties = schema.getProperties();

        properties.forEach((propertyName, propertySchema) -> {
            final var fieldOptional = JTreeFilter.fields(classDeclaration)
                    .stream().filter(it -> it.getName().equals(propertyName))
                    .findFirst();

            fieldOptional.ifPresent(field -> adaptField(field, propertySchema));
        });
    }

    protected void adaptField(final VariableDeclaration field,
                              final Schema<?> propertySchema) {
        addMinAndMaxConstraint(propertySchema, field);
        addSizeConstraint(propertySchema, field);
        addPatternConstraint(propertySchema, field);
        addEmailConstraint(propertySchema, field);
        addAssertTrueOrFalseConstraint(propertySchema, field);
        addDigitsConstraint(propertySchema, field);
        addPastConstraint(propertySchema, field);
        addPastOrPresentConstraint(propertySchema, field);
        addFutureOrPresentConstraint(propertySchema, field);
        addFutureConstraint(propertySchema, field);
        addNullConstraint(propertySchema, field);
        addNotNullConstraint(propertySchema, field);
        addNegativeConstraint(propertySchema, field);
        addNegativeOrZeroConstraint(propertySchema, field);
        addPositiveConstraint(propertySchema, field);
        addPositiveOrZeroConstraint(propertySchema, field);

        processExtensions(field, propertySchema);
    }

    private void processExtensions(final VariableDeclaration field,
                                   final Schema<?> propertySchema) {
        final var extensions = nonNull(propertySchema.getExtensions());

        extensions.forEach((extension, extensionValue) -> {
            final var extensionName = extension.substring(2);

            if ("validation".equals(extensionName)) {
                processValidationExtension(
                        field,
                        (Map<String, Object>) extensionValue
                );
            } else {
                throw new UnsupportedOperationException(extensionName);
            }
        });
    }

    private void processValidationExtension(final VariableDeclaration field,
                                            final Map<String, Object> extensionValue) {
        final var allowedValue = extensionValue.get("allowed-value");

        if ("pastOrPresent".equals(allowedValue)) {
            field.annotation(
                    new AnnotationExpression()
                            .annotationType(new ClassOrInterfaceTypeExpression(constraintsPackage + ".PastOrPresent"))
            );
        }
    }

    private void addMinAndMaxConstraint(final Schema<?> schema,
                                        final VariableDeclaration fieldDeclaration) {
        final boolean isIntegerOrLong = schema.getMinimum() != null
                ? isIntegerOrLong(schema.getMinimum())
                : isIntegerOrLong(schema.getMaximum());

        final var minimum = increment(schema.getMinimum(), schema.getExclusiveMinimum());

        if (minimum != null) {
            final var annotationName = isIntegerOrLong
                    ? constraintsPackage + ".Min"
                    : constraintsPackage + ".DecimalMin";

            fieldDeclaration.annotation(new AnnotationExpression()
                    .annotationType(new ClassOrInterfaceTypeExpression(annotationName))
                    .argument("value", LiteralExpression.createLongLiteralExpression(minimum))
            );
        }

        final var maximum = decrementAndToString(schema.getMaximum(), schema.getExclusiveMaximum());

        if (maximum != null) {
            final var annotationName = isIntegerOrLong
                    ? constraintsPackage + ".Max"
                    : constraintsPackage + ".DecimalMax";

            fieldDeclaration.annotation(
                    new AnnotationExpression()
                            .annotationType(new ClassOrInterfaceTypeExpression(annotationName))
                            .argument("value", LiteralExpression.createLongLiteralExpression(maximum))
            );
        }
    }

    private boolean isIntegerOrLong(final Number number) {
        if (number instanceof Integer || number instanceof Long) {
            return true;
        } else {
            final var bigDecimal = (BigDecimal) number;
            try {
                bigDecimal.longValueExact();
                return true;
            } catch (final ArithmeticException ignored) {
                return false;
            }
        }
    }

    private void addSizeConstraint(final Schema<?> schema,
                                   final VariableDeclaration fieldDeclaration) {
        final Integer min;
        final Integer max;

        if (schema instanceof StringSchema) {
            min = schema.getMinLength();
            max = schema.getMaxLength();
        } else {
            min = schema.getMinItems();
            max = schema.getMaxItems();
        }

        if (min != null || max != null) {
            final var sizeAnnotation = new AnnotationExpression(new ClassOrInterfaceTypeExpression(constraintsPackage + ".Size"));

            if (min != null
                    && min > 0) {
                sizeAnnotation.argument("min", LiteralExpression.createIntLiteralExpression(min));
            }

            if (max != null
                    && max < Integer.MAX_VALUE) {
                sizeAnnotation.argument("max", LiteralExpression.createIntLiteralExpression(max));
            }

            fieldDeclaration.annotation(sizeAnnotation);
        }
    }

    private void addPatternConstraint(final Schema<?> schema,
                                      final VariableDeclaration fieldDeclaration) {
        final var pattern = schema.getPattern();

        if (pattern != null) {
            fieldDeclaration.annotation(
                    new AnnotationExpression(new ClassOrInterfaceTypeExpression(constraintsPackage + ".Pattern"))
                            .argument("regexp", LiteralExpression.createStringLiteralExpression(pattern))
            );
        }
    }

    private void addEmailConstraint(final Schema<?> schema,
                                    final VariableDeclaration fieldDeclaration) {
        if ("email".equals(schema.getFormat())) {
            final var emailAnnotation = new AnnotationExpression(new ClassOrInterfaceTypeExpression(constraintsPackage + ".Email"));

            final var pattern = schema.getPattern();

            if (pattern != null) {
                emailAnnotation.argument("regexp", LiteralExpression.createStringLiteralExpression(pattern));
            }

            fieldDeclaration.annotation(emailAnnotation);
        }
    }

    private @Nullable Long increment(final @Nullable Number number,
                                     final @Nullable Boolean exclusiveMinimum) {
        if (number == null) {
            return null;
        } else if (Boolean.TRUE.equals(exclusiveMinimum)) {
            return number instanceof Integer i
                    ? Integer.valueOf(i + 1).longValue()
                    : ((Long) number) + 1L;
        } else {
            return number.longValue();
        }
    }

    private @Nullable Long decrementAndToString(final @Nullable Number number,
                                                final @Nullable Boolean exclusiveMaximum) {
        if (number == null) {
            return null;
        } else if (Boolean.TRUE.equals(exclusiveMaximum)) {
            return number instanceof Integer i
                    ? Integer.valueOf(i - 1).longValue()
                    : ((Long) number) - 1L;
        } else {
            return number.longValue();
        }
    }

    private void addAssertTrueOrFalseConstraint(final Schema<?> schema,
                                                final VariableDeclaration fieldDeclaration) {
        final var fieldType = fieldDeclaration.getVarType().getType();
        if (isBooleanType(fieldType)) {
            final var extensions = nonNull(schema.getExtensions());
            final var xAssert = extensions.get(Extensions.ASSERT);

            if (Boolean.TRUE.equals(xAssert)) {
                fieldDeclaration.annotation(new AnnotationExpression(constraintsPackage + ".AssertTrue"));
            } else if (Boolean.FALSE.equals(xAssert)) {
                fieldDeclaration.annotation(new AnnotationExpression(constraintsPackage + ".AssertFalse"));
            }
        }
    }

    private boolean isBooleanType(final TypeMirror type) {
        if (type instanceof PrimitiveType primitiveType) {
            return primitiveType.getKind().isPrimitive();
        } else if (type instanceof DeclaredType declaredType) {
            return "java.lang.Boolean".equals(getQualifiedNameOf(declaredType.asElement()).toString());
        } else {
            return false;
        }
    }

    private void addDigitsConstraint(final Schema<?> schema,
                                     final VariableDeclaration field) {
        final var digitsOptional = ValidationExtensions.digits(schema.getExtensions());

        digitsOptional.ifPresent(digits -> {
            final var annotation = new AnnotationExpression(constraintsPackage + ".Digits")
                    .argument("integer", LiteralExpression.createIntLiteralExpression(digits.integer()))
                    .argument("fraction", LiteralExpression.createIntLiteralExpression(digits.fraction()));
            field.annotation(annotation);
        });
    }

    private void addPastConstraint(final Schema<?> schema,
                                   final VariableDeclaration field) {
        final var allowedValue = ValidationExtensions.allowedValue(schema.getExtensions());

        if ("past".equals(allowedValue)) {
            field.annotation(new AnnotationExpression(constraintsPackage + ".Past"));
        }
    }

    private void addPastOrPresentConstraint(final Schema<?> schema,
                                            final VariableDeclaration field) {
        final var allowedValue = ValidationExtensions.allowedValue(schema.getExtensions());

        if ("pastOrPresent".equals(allowedValue)) {
            field.annotation(new AnnotationExpression(constraintsPackage + ".PastOrPresent"));
        }
    }

    private void addFutureOrPresentConstraint(final Schema<?> schema,
                                              final VariableDeclaration field) {
        final var allowedValue = ValidationExtensions.allowedValue(schema.getExtensions());

        if ("future".equals(allowedValue)) {
            field.annotation(new AnnotationExpression(constraintsPackage + ".addFutureOrPresent"));
        }
    }

    private void addFutureConstraint(final Schema<?> schema,
                                     final VariableDeclaration field) {
        final var allowedValue = ValidationExtensions.allowedValue(schema.getExtensions());
        if ("future".equals(allowedValue)) {
            field.annotation(new AnnotationExpression(constraintsPackage + ".Future"));
        }
    }

    private void addNullConstraint(final Schema<?> schema,
                                   final VariableDeclaration field) {
        final var extensions = nonNull(schema.getExtensions());

        if (extensions.containsKey(Extensions.ASSERT)
                && extensions.get(Extensions.ASSERT) == null) {
            field.annotation(constraintsPackage + ".Null");
        }
    }

    private void addNotNullConstraint(final Schema<?> schema,
                                      final VariableDeclaration field) {
        if (Boolean.FALSE.equals(schema.getNullable())) {
            field.annotation(constraintsPackage + ".NotNull");
        }
    }

    private void addNegativeConstraint(final Schema<?> schema,
                                       final VariableDeclaration field) {
        final var extensions = nonNull(schema.getExtensions());
        if ("negative".equals(extensions.get(Extensions.ASSERT))) {
            field.annotation(constraintsPackage + ".Negative");
        }
    }

    private void addNegativeOrZeroConstraint(final Schema<?> schema,
                                             final VariableDeclaration field) {
        final var extensions = nonNull(schema.getExtensions());
        if ("negativeOrZero".equals(extensions.get(Extensions.ASSERT))) {
            field.annotation(constraintsPackage + ".NegativeOrZero");
        }
    }

    private void addPositiveConstraint(final Schema<?> schema,
                                       final VariableDeclaration field) {
        final var extensions = nonNull(schema.getExtensions());
        if ("positive".equals(extensions.get(Extensions.ASSERT))) {
            field.annotation(constraintsPackage + ".Positive");
        }
    }

    private void addPositiveOrZeroConstraint(final Schema<?> schema,
                                             final VariableDeclaration field) {
        final var extensions = nonNull(schema.getExtensions());
        if ("positiveOrZero".equals(extensions.get(Extensions.ASSERT))) {
            field.annotation(constraintsPackage + ".PositiveOrZero");
        }
    }

    private Map<String, Object> nonNull(final Map<String, Object> map) {
        return map != null
                ? map
                : Map.of();
    }

}
