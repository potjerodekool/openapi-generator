package io.github.potjerodekool.openapi.common.generate.model.adapter;

import io.github.potjerodekool.openapi.common.generate.model.element.Annotation;
import io.github.potjerodekool.openapi.common.generate.model.element.AnnotationTarget;
import io.github.potjerodekool.openapi.common.generate.model.element.Model;
import io.github.potjerodekool.openapi.common.generate.model.element.ModelProperty;
import io.github.potjerodekool.openapi.common.generate.model.expresion.LiteralExpression;
import io.github.potjerodekool.openapi.common.generate.ValidationExtensions;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.math.BigDecimal;
import java.util.Map;

public class JakartaValidationModelAdapter implements ModelAdapter {

    private final String constraintsPackage;

    public JakartaValidationModelAdapter() {
        final var validationBasePackage = "jakarta" + ".validation";
        this.constraintsPackage = validationBasePackage + ".constraints";
    }

    @Override
    public void adapt(final Model model, final ObjectSchema schema) {
        schema.getProperties().forEach((propertyName, propertySchema) -> {
            final var propertyOptional = model.getProperty(propertyName);
            propertyOptional.ifPresent(property -> adaptProperty(propertySchema, property));
        });
    }

    protected void adaptProperty(final Schema<?> propertySchema,
                                 final ModelProperty property) {
        addMinAndMaxConstraint(propertySchema, property);
        addSizeConstraint(propertySchema, property);
        addPatternConstraint(propertySchema, property);
        addEmailConstraint(propertySchema, property);
        addAssertTrueOrFalseConstraint(propertySchema, property);
        addDigitsConstraint(propertySchema, property);
        addPastConstraint(propertySchema, property);
        addPastOrPresentConstraint(propertySchema, property);
        addFutureOrPresentConstraint(propertySchema, property);
        addFutureConstraint(propertySchema, property);
        addNullConstraint(propertySchema, property);
        addNotNullConstraint(propertySchema, property);
        addNegativeConstraint(propertySchema, property);
        addNegativeOrZeroConstraint(propertySchema, property);
        addPositiveConstraint(propertySchema, property);
        addPositiveOrZeroConstraint(propertySchema, property);

        processExtensions(propertySchema, property);
    }

    private void addMinAndMaxConstraint(final Schema<?> schema,
                                        final ModelProperty property) {
        final boolean isIntegerOrLong = schema.getMinimum() != null
                ? isIntegerOrLong(schema.getMinimum())
                : isIntegerOrLong(schema.getMaximum());

        final var minimum = increment(schema.getMinimum(), schema.getExclusiveMinimum());

        if (minimum != null) {
            final var annotationName = isIntegerOrLong
                    ? constraintsPackage + ".Min"
                    : constraintsPackage + ".DecimalMin";

            property.annotation(
                    new Annotation()
                            .name(annotationName)
                            .attribute("value", new LiteralExpression(
                                    minimum
                            ))
                            .annotationTarget(AnnotationTarget.FIELD)
            );
        }

        final var maximum = decrementAndToString(schema.getMaximum(), schema.getExclusiveMaximum());

        if (maximum != null) {
            final var annotationName = isIntegerOrLong
                    ? constraintsPackage + ".Max"
                    : constraintsPackage + ".DecimalMax";

            property.annotation(
                    new Annotation()
                            .name(annotationName)
                            .attribute("value", new LiteralExpression(maximum))
                            .annotationTarget(AnnotationTarget.FIELD)
            );
        }
    }

    private void addSizeConstraint(final Schema<?> schema,
                                   final ModelProperty property) {
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
            final var sizeAnnotation = new Annotation()
                    .name(constraintsPackage + ".Size")
                    .annotationTarget(AnnotationTarget.FIELD);

            if (min != null
                    && min > 0) {
                sizeAnnotation.attribute("min", new LiteralExpression(min));
            }

            if (max != null
                    && max < Integer.MAX_VALUE) {
                sizeAnnotation.attribute("max", max);
            }

            property.annotation(sizeAnnotation);
        }
    }

    private void addPatternConstraint(final Schema<?> schema,
                                      final ModelProperty property) {
        final var pattern = schema.getPattern();

        if (pattern != null) {
            property.annotation(
                    new Annotation()
                            .name(constraintsPackage + ".Pattern")
                            .attribute("regexp", new LiteralExpression(pattern))
                            .annotationTarget(AnnotationTarget.FIELD)
            );
        }
    }

    private void addEmailConstraint(final Schema<?> schema,
                                    final ModelProperty property) {
        if ("email".equals(schema.getFormat())) {
            final var emailAnnotation = new Annotation()
                    .name(constraintsPackage + ".Email")
                    .annotationTarget(AnnotationTarget.FIELD);

            final var pattern = schema.getPattern();

            if (pattern != null) {
                emailAnnotation.attribute("regexp", new LiteralExpression(pattern));
            }

            property.annotation(emailAnnotation);
        }
    }

    private void addAssertTrueOrFalseConstraint(final Schema<?> schema,
                                                final ModelProperty property) {
        if (schema instanceof BooleanSchema) {
            final var extensions = nonNull(schema.getExtensions());
            final var xAssert = extensions.get(ValidationExtensions.ASSERT);

            if (Boolean.TRUE.equals(xAssert)) {
                property.annotation(
                        new Annotation()
                                .name(constraintsPackage + ".AssertTrue")
                                .annotationTarget(AnnotationTarget.FIELD)
                );

            } else if (Boolean.FALSE.equals(xAssert)) {
                property.annotation(
                        new Annotation()
                                .name(constraintsPackage + ".AssertFalse")
                                .annotationTarget(AnnotationTarget.FIELD)
                );
            }
        }
    }

    private void addDigitsConstraint(final Schema<?> schema,
                                     final ModelProperty property) {
        final var digitsOptional = ValidationExtensions.digits(schema.getExtensions());

        digitsOptional.ifPresent(digits -> {
            final var annotation = new Annotation()
                    .name(constraintsPackage + ".Digits")
                    .attribute("integer", new LiteralExpression(digits.integer()))
                    .attribute("fraction", new LiteralExpression(digits.fraction()))
                    .annotationTarget(AnnotationTarget.FIELD);
            property.annotation(annotation);
        });
    }

    private void addPastConstraint(final Schema<?> schema,
                                   final ModelProperty property) {
        final var allowedValue = ValidationExtensions.allowedValue(schema.getExtensions());

        if ("past".equals(allowedValue)) {
            property.annotation(new Annotation()
                    .name(constraintsPackage + ".Past")
                    .annotationTarget(AnnotationTarget.FIELD)
            );
        }
    }

    private void addPastOrPresentConstraint(final Schema<?> schema,
                                            final ModelProperty property) {
        final var allowedValue = ValidationExtensions.allowedValue(schema.getExtensions());

        if ("pastOrPresent".equals(allowedValue)) {
            property.annotation(
                    new Annotation()
                            .name(constraintsPackage + ".PastOrPresent")
                            .annotationTarget(AnnotationTarget.FIELD)
            );
        }
    }

    private void addFutureOrPresentConstraint(final Schema<?> schema,
                                              final ModelProperty property) {
        final var allowedValue = ValidationExtensions.allowedValue(schema.getExtensions());

        if ("future".equals(allowedValue)) {
            property.annotation(
                    new Annotation()
                            .name(constraintsPackage + ".FutureOrPresent")
                            .annotationTarget(AnnotationTarget.FIELD)
            );
        }
    }

    private void addFutureConstraint(final Schema<?> schema,
                                     final ModelProperty property) {
        final var allowedValue = ValidationExtensions.allowedValue(schema.getExtensions());
        if ("future".equals(allowedValue)) {
            property.annotation(new Annotation()
                    .name(constraintsPackage + ".Future")
                    .annotationTarget(AnnotationTarget.FIELD)
            );
        }
    }

    private void addNullConstraint(final Schema<?> schema,
                                   final ModelProperty property) {
        final var extensions = nonNull(schema.getExtensions());

        if (extensions.containsKey(ValidationExtensions.ASSERT)
                && extensions.get(ValidationExtensions.ASSERT) == null) {
            property.annotation(new Annotation()
                    .name(constraintsPackage + ".Null")
                    .annotationTarget(AnnotationTarget.FIELD)
            );
        }
    }

    private void addNotNullConstraint(final Schema<?> schema,
                                      final ModelProperty property) {
        if (Boolean.FALSE.equals(schema.getNullable())) {
            property.annotation(new Annotation()
                    .name(constraintsPackage + ".NotNull")
                    .annotationTarget(AnnotationTarget.FIELD)
            );
        }
    }

    private void addNegativeConstraint(final Schema<?> schema,
                                       final ModelProperty property) {
        final var extensions = nonNull(schema.getExtensions());
        if ("negative".equals(extensions.get(ValidationExtensions.ASSERT))) {
            property.annotation(new Annotation()
                    .name(constraintsPackage + ".Negative")
                    .annotationTarget(AnnotationTarget.FIELD)
            );
        }
    }

    private void addNegativeOrZeroConstraint(final Schema<?> schema,
                                             final ModelProperty property) {
        final var extensions = nonNull(schema.getExtensions());
        if ("negativeOrZero".equals(extensions.get(ValidationExtensions.ASSERT))) {
            property.annotation(new Annotation()
                    .name(constraintsPackage + ".NegativeOrZero")
                    .annotationTarget(AnnotationTarget.FIELD)
            );
        }
    }

    private void addPositiveConstraint(final Schema<?> schema,
                                       final ModelProperty property) {
        final var extensions = nonNull(schema.getExtensions());
        if ("positive".equals(extensions.get(ValidationExtensions.ASSERT))) {
            property.annotation(new Annotation()
                    .name(constraintsPackage + ".Positive")
                    .annotationTarget(AnnotationTarget.FIELD)
            );
        }
    }

    private void addPositiveOrZeroConstraint(final Schema<?> schema,
                                             final ModelProperty property) {
        final var extensions = nonNull(schema.getExtensions());
        if ("positiveOrZero".equals(extensions.get(ValidationExtensions.ASSERT))) {
            property.annotation(new Annotation()
                    .name(constraintsPackage + ".PositiveOrZero")
                    .annotationTarget(AnnotationTarget.FIELD)
            );
        }
    }

    private void processExtensions(final Schema<?> propertySchema,
                                   final ModelProperty property) {
        final var extensions = nonNull(propertySchema.getExtensions());

        extensions.forEach((extension, extensionValue) -> {
            final var extensionName = extension.substring(2);

            if ("validation".equals(extensionName)) {
                processValidationExtension(
                        (Map<String, Object>) extensionValue,
                        property
                );
            } else {
                throw new UnsupportedOperationException(extensionName);
            }
        });
    }

    private void processValidationExtension(final Map<String, Object> extensionValue,
                                            final ModelProperty property) {
        final var allowedValue = extensionValue.get("allowed-value");

        if ("pastOrPresent".equals(allowedValue)) {
            property.annotation(new Annotation()
                    .name(constraintsPackage + ".PastOrPresent")
                    .annotationTarget(AnnotationTarget.FIELD)
            );
        }
    }

    private boolean isIntegerOrLong(final Number number) {
        if (number == null) {
            return false;
        } else if (number instanceof Integer || number instanceof Long) {
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

    private Map<String, Object> nonNull(final Map<String, Object> map) {
        return map != null
                ? map
                : Map.of();
    }
}
