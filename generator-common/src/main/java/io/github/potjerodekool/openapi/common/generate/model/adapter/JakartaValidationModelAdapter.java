package io.github.potjerodekool.openapi.common.generate.model.adapter;

import io.github.potjerodekool.codegen.template.model.annotation.Annot;
import io.github.potjerodekool.codegen.template.model.annotation.AnnotTarget;
import io.github.potjerodekool.codegen.template.model.expression.SimpleLiteralExpr;
import io.github.potjerodekool.openapi.common.generate.model.element.Model;
import io.github.potjerodekool.openapi.common.generate.model.element.ModelProperty;
import io.github.potjerodekool.openapi.common.generate.ValidationExtensions;
import io.swagger.v3.oas.models.media.BooleanSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.math.BigDecimal;
import java.util.Map;

public class JakartaValidationModelAdapter implements ValidationModelAdapter {

    private final String constraintsPackage;

    public JakartaValidationModelAdapter() {
        final var validationBasePackage = "jakarta" + ".validation";
        this.constraintsPackage = validationBasePackage + ".constraints";
    }

    @Override
    public void adapt(final Model model, final ObjectSchema schema) {
        final var properties = schema.getProperties();

        if (properties != null) {
            properties.forEach((propertyName, propertySchema) -> {
                final var propertyOptional = model.getProperty(propertyName);
                propertyOptional.ifPresent(property -> adaptProperty(propertySchema, property));
            });
        }
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
                    new Annot()
                            .name(annotationName)
                            .attribute("value", new SimpleLiteralExpr(
                                    minimum
                            ))
                            .target(AnnotTarget.FIELD)
            );
        }

        final var maximum = decrementAndToString(schema.getMaximum(), schema.getExclusiveMaximum());

        if (maximum != null) {
            final var annotationName = isIntegerOrLong
                    ? constraintsPackage + ".Max"
                    : constraintsPackage + ".DecimalMax";

            property.annotation(
                    new Annot()
                            .name(annotationName)
                            .value("value", new SimpleLiteralExpr(maximum))
                            .target(AnnotTarget.FIELD)
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
            final var sizeAnnotation = new Annot()
                    .name(constraintsPackage + ".Size")
                    .target(AnnotTarget.FIELD);

            if (min != null
                    && min > 0) {
                sizeAnnotation.attribute("min", new SimpleLiteralExpr(min));
            }

            if (max != null
                    && max < Integer.MAX_VALUE) {
                sizeAnnotation.attribute("max", new SimpleLiteralExpr(max));
            }

            property.annotation(sizeAnnotation);
        }
    }

    private void addPatternConstraint(final Schema<?> schema,
                                      final ModelProperty property) {
        final var pattern = schema.getPattern();

        if (pattern != null) {
            property.annotation(
                    new Annot()
                            .name(constraintsPackage + ".Pattern")
                            .attribute("regexp", new SimpleLiteralExpr(pattern))
                            .target(AnnotTarget.FIELD)
            );
        }
    }

    private void addEmailConstraint(final Schema<?> schema,
                                    final ModelProperty property) {
        if ("email".equals(schema.getFormat())) {
            final var emailAnnotation = new Annot()
                    .name(constraintsPackage + ".Email")
                    .target(AnnotTarget.FIELD);

            final var pattern = schema.getPattern();

            if (pattern != null) {
                emailAnnotation.attribute("regexp", new SimpleLiteralExpr(pattern));
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
                        new Annot()
                                .name(constraintsPackage + ".AssertTrue")
                                .target(AnnotTarget.FIELD)
                );

            } else if (Boolean.FALSE.equals(xAssert)) {
                property.annotation(
                        new Annot()
                                .name(constraintsPackage + ".AssertFalse")
                                .target(AnnotTarget.FIELD)
                );
            }
        }
    }

    private void addDigitsConstraint(final Schema<?> schema,
                                     final ModelProperty property) {
        final var digitsOptional = ValidationExtensions.digits(schema.getExtensions());

        digitsOptional.ifPresent(digits -> {
            final var annotation = new Annot()
                    .name(constraintsPackage + ".Digits")
                    .attribute("integer", new SimpleLiteralExpr(digits.integer()))
                    .attribute("fraction", new SimpleLiteralExpr(digits.fraction()))
                    .target(AnnotTarget.FIELD);
            property.annotation(annotation);
        });
    }

    private void addPastConstraint(final Schema<?> schema,
                                   final ModelProperty property) {
        final var allowedValue = ValidationExtensions.allowedValue(schema.getExtensions());

        if ("past".equals(allowedValue)) {
            property.annotation(new Annot()
                    .name(constraintsPackage + ".Past")
                    .target(AnnotTarget.FIELD)
            );
        }
    }

    private void addPastOrPresentConstraint(final Schema<?> schema,
                                            final ModelProperty property) {
        final var allowedValue = ValidationExtensions.allowedValue(schema.getExtensions());

        if ("pastOrPresent".equals(allowedValue)) {
            property.annotation(
                    new Annot()
                            .name(constraintsPackage + ".PastOrPresent")
                            .target(AnnotTarget.FIELD)
            );
        }
    }

    private void addFutureOrPresentConstraint(final Schema<?> schema,
                                              final ModelProperty property) {
        final var allowedValue = ValidationExtensions.allowedValue(schema.getExtensions());

        if ("future".equals(allowedValue)) {
            property.annotation(
                    new Annot()
                            .name(constraintsPackage + ".FutureOrPresent")
                            .target(AnnotTarget.FIELD)
            );
        }
    }

    private void addFutureConstraint(final Schema<?> schema,
                                     final ModelProperty property) {
        final var allowedValue = ValidationExtensions.allowedValue(schema.getExtensions());
        if ("future".equals(allowedValue)) {
            property.annotation(new Annot()
                    .name(constraintsPackage + ".Future")
                    .target(AnnotTarget.FIELD)
            );
        }
    }

    private void addNullConstraint(final Schema<?> schema,
                                   final ModelProperty property) {
        final var extensions = nonNull(schema.getExtensions());

        if (extensions.containsKey(ValidationExtensions.ASSERT)
                && extensions.get(ValidationExtensions.ASSERT) == null) {
            property.annotation(new Annot()
                    .name(constraintsPackage + ".Null")
                    .target(AnnotTarget.FIELD)
            );
        }
    }

    private void addNotNullConstraint(final Schema<?> schema,
                                      final ModelProperty property) {
        if (Boolean.FALSE.equals(schema.getNullable())) {
            property.annotation(new Annot()
                    .name(constraintsPackage + ".NotNull")
                    .target(AnnotTarget.FIELD)
            );
        }
    }

    private void addNegativeConstraint(final Schema<?> schema,
                                       final ModelProperty property) {
        final var extensions = nonNull(schema.getExtensions());
        if ("negative".equals(extensions.get(ValidationExtensions.ASSERT))) {
            property.annotation(new Annot()
                    .name(constraintsPackage + ".Negative")
                    .target(AnnotTarget.FIELD)
            );
        }
    }

    private void addNegativeOrZeroConstraint(final Schema<?> schema,
                                             final ModelProperty property) {
        final var extensions = nonNull(schema.getExtensions());
        if ("negativeOrZero".equals(extensions.get(ValidationExtensions.ASSERT))) {
            property.annotation(new Annot()
                    .name(constraintsPackage + ".NegativeOrZero")
                    .target(AnnotTarget.FIELD)
            );
        }
    }

    private void addPositiveConstraint(final Schema<?> schema,
                                       final ModelProperty property) {
        final var extensions = nonNull(schema.getExtensions());
        if ("positive".equals(extensions.get(ValidationExtensions.ASSERT))) {
            property.annotation(new Annot()
                    .name(constraintsPackage + ".Positive")
                    .target(AnnotTarget.FIELD)
            );
        }
    }

    private void addPositiveOrZeroConstraint(final Schema<?> schema,
                                             final ModelProperty property) {
        final var extensions = nonNull(schema.getExtensions());
        if ("positiveOrZero".equals(extensions.get(ValidationExtensions.ASSERT))) {
            property.annotation(new Annot()
                    .name(constraintsPackage + ".PositiveOrZero")
                    .target(AnnotTarget.FIELD)
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
            }
        });
    }

    private void processValidationExtension(final Map<String, Object> extensionValue,
                                            final ModelProperty property) {
        final var allowedValue = extensionValue.get("allowed-value");

        if ("pastOrPresent".equals(allowedValue)) {
            property.annotation(new Annot()
                    .name(constraintsPackage + ".PastOrPresent")
                    .target(AnnotTarget.FIELD)
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
