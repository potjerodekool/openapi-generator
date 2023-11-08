package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.expression.LiteralExpression;
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.codegen.model.type.DeclaredType;
import io.github.potjerodekool.codegen.model.type.PrimitiveType;
import io.github.potjerodekool.codegen.model.type.TypeKind;
import io.github.potjerodekool.codegen.model.type.TypeMirror;
import io.github.potjerodekool.codegen.model.util.Elements;
import io.github.potjerodekool.codegen.model.util.type.Types;
import io.github.potjerodekool.openapi.Features;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.generate.model.ModelAdapter;
import io.github.potjerodekool.openapi.internal.di.Bean;
import io.github.potjerodekool.openapi.internal.di.ConditionalOnMissingBean;
import io.github.potjerodekool.openapi.internal.util.CollectionBuilder;
import io.github.potjerodekool.openapi.tree.Extensions;
import io.github.potjerodekool.openapi.tree.media.OpenApiSchema;
import io.github.potjerodekool.openapi.tree.media.OpenApiStringSchema;
import jakarta.inject.Inject;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.Map;
import java.util.Set;

import static io.github.potjerodekool.codegen.model.element.Name.getQualifiedNameOf;
import static io.github.potjerodekool.openapi.internal.ClassNames.*;

@Bean
@ConditionalOnMissingBean
//https://jakarta.ee/specifications/bean-validation/3.0/jakarta-bean-validation-spec-3.0.html
public class ValidationModelAdapter implements ModelAdapter {

    private static final String MAP_CLASS_NAME = "java.util.Map";

    private final Set<TypeMirror> minMaxTypes;

    private final TypeMirror charSequenceType;

    private final Set<TypeMirror> digitsTypes;

    private final Set<TypeMirror> sizeTypes;

    private final Set<TypeMirror> futureTypes;

    private final String constraintsPackage;

    private final Types types;

    @Inject
    public ValidationModelAdapter(final GeneratorConfig generatorConfig,
                                  final Environment environment) {
        final var types = environment.getTypes();
        final var elements = environment.getElementUtils();

        final var validationBasePackage = (generatorConfig.isFeatureEnabled(Features.FEATURE_JAKARTA) ? "jakarta" : "javax") + ".validation";
        this.constraintsPackage = validationBasePackage + ".constraints";
        this.types = environment.getTypes();
        this.charSequenceType = types.getDeclaredType(elements.getTypeElement(CHAR_SEQUENCE_NAME));

        this.minMaxTypes = initMinMaxTypes(types, environment.getElementUtils());
        this.sizeTypes = initSizeTypes(environment.getElementUtils());
        this.digitsTypes = initDigitsTypes(types, environment.getElementUtils());
        this.futureTypes = initFutureTypes(types, environment.getElementUtils());
    }

    private static Set<TypeMirror> initMinMaxTypes(final Types types,
                                                   final Elements elements) {
        return Set.of(
                types.getDeclaredType(elements.getTypeElement("java.math.BigDecimal")),
                types.getDeclaredType(elements.getTypeElement("java.math.BigInteger")),
                types.getPrimitiveType(TypeKind.BYTE),
                types.getDeclaredType(elements.getTypeElement("java.lang.Byte")),
                types.getPrimitiveType(TypeKind.SHORT),
                types.getDeclaredType(elements.getTypeElement("java.lang.Short")),
                types.getPrimitiveType(TypeKind.INT),
                types.getDeclaredType(elements.getTypeElement("java.lang.Integer")),
                types.getPrimitiveType(TypeKind.LONG),
                types.getDeclaredType(elements.getTypeElement("java.lang.Long"))
        );
    }

    private static Set<TypeMirror> initSizeTypes(final Elements elements) {
        return Set.of(
                elements.getTypeElement(LIST_CLASS_NAME).asType(),
                elements.getTypeElement(MAP_CLASS_NAME).asType()
        );
    }

    private static Set<TypeMirror> initDigitsTypes(final Types types,
                                                   final Elements elements) {
        return Set.of(
                elements.getTypeElement("java.math.BigDecimal").asType(),
                elements.getTypeElement("java.math.BigInteger").asType(),
                types.getPrimitiveType(TypeKind.BYTE),
                elements.getTypeElement("java.lang.Byte").asType(),
                types.getPrimitiveType(TypeKind.SHORT),
                elements.getTypeElement("java.lang.Short").asType(),
                types.getPrimitiveType(TypeKind.INT),
                elements.getTypeElement("java.lang.Integer").asType(),
                types.getPrimitiveType(TypeKind.LONG),
                elements.getTypeElement("java.lang.Long").asType()
        );
    }

    private static Set<TypeMirror> initFutureTypes(final Types types,
                                                   final Elements elements) {
        return new CollectionBuilder<TypeMirror>()
                .add(types.getDeclaredType(elements.getTypeElement("java.util.Date")))
                .add(types.getDeclaredType(elements.getTypeElement("java.util.Calendar")))
                .add(types.getDeclaredType(elements.getTypeElement("java.time.Instant")))
                .add(types.getDeclaredType(elements.getTypeElement("java.time.LocalDate")))
                .add(types.getDeclaredType(elements.getTypeElement("java.time.LocalDateTime")))
                .add(types.getDeclaredType(elements.getTypeElement("java.time.MonthDay")))
                .add(types.getDeclaredType(elements.getTypeElement("java.time.OffsetDateTime")))
                .add(types.getDeclaredType(elements.getTypeElement("java.time.OffsetTime")))
                .add(types.getDeclaredType(elements.getTypeElement("java.time.Year")))
                .add(types.getDeclaredType(elements.getTypeElement("java.time.YearMonth")))
                .add(types.getDeclaredType(elements.getTypeElement("java.time.ZonedDateTime")))
                .add(types.getDeclaredType(elements.getTypeElement("java.time.chrono.HijrahDate")))
                .add(types.getDeclaredType(elements.getTypeElement("java.time.chrono.JapaneseDate")))
                .add(types.getDeclaredType(elements.getTypeElement("java.time.chrono.JapaneseDate")))
                .add(types.getDeclaredType(elements.getTypeElement("java.time.chrono.MinguoDate")))
                .add(types.getDeclaredType(elements.getTypeElement("java.time.chrono.ThaiBuddhistDate")))
                .buildSet();
    }

    @Override
    public void adaptField(final VariableDeclaration<?> fieldDeclaration) {
        final var schema = (OpenApiSchema<?>) fieldDeclaration.getMetaData(OpenApiSchema.class.getSimpleName());

        addMinAndMaxConstraint(schema, fieldDeclaration);
        addSizeConstraint(schema, fieldDeclaration);
        addPatternConstraint(schema, fieldDeclaration);
        addEmailConstraint(schema, fieldDeclaration);
        addAssertTrueOrFalseConstraint(schema, fieldDeclaration);
        addDigitsConstraint(schema, fieldDeclaration);
        addPastConstraint(schema, fieldDeclaration);
        addPastOrPresentConstraint(schema, fieldDeclaration);
        addFutureOrPresentConstraint(schema, fieldDeclaration);
        addFutureConstraint(schema, fieldDeclaration);
        addNullConstraint(schema, fieldDeclaration);
        addNotNullConstraint(schema, fieldDeclaration);
        addNegativeConstraint(schema, fieldDeclaration);
        addNegativeOrZeroConstraint(schema, fieldDeclaration);
        addPositiveConstraint(schema, fieldDeclaration);
        addPositiveOrZeroConstraint(schema, fieldDeclaration);
    }

    private void addMinAndMaxConstraint(final OpenApiSchema<?> schema,
                                        final VariableDeclaration<?> fieldDeclaration) {
        final var fieldType = fieldDeclaration.getVarType().getType();

        if (!minMaxTypes.contains(fieldType)) {
            return;
        }

        final boolean isIntegerOrLong = schema.minimum() != null
            ? isIntegerOrLong(schema.minimum())
            : isIntegerOrLong(schema.maximum());

        final var minimum = increment(schema.minimum(), schema.exclusiveMinimum());

        if (minimum != null) {
            final String annotationName;

            if (isIntegerOrLong) {
                annotationName = constraintsPackage + ".Min";
            } else {
                annotationName = constraintsPackage + ".DecimalMin";
            }

            fieldDeclaration.annotation(new AnnotationExpression(
                    new ClassOrInterfaceTypeExpression(annotationName),
                    LiteralExpression.createLongLiteralExpression(minimum)
                 )
            );
        }

        final var maximum = decrementAndToString(schema.maximum(), schema.exclusiveMaximum());

        if (maximum != null) {
            final String annotationName;

            if (isIntegerOrLong) {
                annotationName = constraintsPackage + ".Max";
            } else {
                annotationName = constraintsPackage + ".DecimalMax";
            }

            fieldDeclaration.annotation(
                    new AnnotationExpression(
                            new ClassOrInterfaceTypeExpression(annotationName),
                            Map.of(
                                    "value", LiteralExpression.createLongLiteralExpression(maximum)
                            )
                    )
            );
        }
    }

    private boolean isIntegerOrLong(final Number number) {
        return number instanceof Integer || number instanceof Long;
    }

    private void addSizeConstraint(final OpenApiSchema<?> schema,
                                   final VariableDeclaration<?> fieldDeclaration) {
        final var fieldType = fieldDeclaration.getVarType();

        if (!isCharSequenceField(fieldDeclaration) &&
                !sizeTypes.contains(fieldType.getType())) {
            return;
        }

        final Integer min;
        final Integer max;

        if (schema instanceof OpenApiStringSchema) {
            min = schema.minLength();
            max = schema.maxLength();
        } else {
            min = schema.minItems();
            max = schema.maxItems();
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

    private void addPatternConstraint(final OpenApiSchema<?> schema,
                                      final VariableDeclaration<?> fieldDeclaration) {
        if (!isCharSequenceField(fieldDeclaration)) {
            return;
        }

        final var pattern = schema.pattern();

        if (pattern != null) {
            fieldDeclaration.annotation(
                    new AnnotationExpression(new ClassOrInterfaceTypeExpression(constraintsPackage + ".Pattern"))
                            .argument("regexp", LiteralExpression.createStringLiteralExpression(pattern))
            );
        }
    }

    private void addEmailConstraint(final OpenApiSchema<?> schema,
                                    final VariableDeclaration<?> fieldDeclaration) {
        if (!isCharSequenceField(fieldDeclaration)) {
            return;
        }

        if ("email".equals(schema.format())) {
            final var emailAnnotation = new AnnotationExpression(new ClassOrInterfaceTypeExpression(constraintsPackage + ".Email"));

            final var pattern = schema.pattern();

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
                    : ((Long)number) - 1L;
        } else {
            return number.longValue();
        }
    }

    private boolean isCharSequenceField(final VariableDeclaration<?> fieldDeclaration) {
        final var fieldType = fieldDeclaration.getVarType();
        return types.isAssignable(charSequenceType, fieldType.getType());
    }

    private void addAssertTrueOrFalseConstraint(final OpenApiSchema<?> schema,
                                                final VariableDeclaration<?> fieldDeclaration) {
        final var fieldType = fieldDeclaration.getVarType().getType();
        if (isBooleanType(fieldType)) {
            final var xAssert = schema.extensions().get(Extensions.ASSERT);

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

    private void addDigitsConstraint(final OpenApiSchema<?> schema,
                                     final VariableDeclaration<?> field) {
        if (!supportsDigits(field.getVarType().getType())) {
            return;
        }

        final var digitsOptional = ValidationExtensions.digits(schema.extensions());

        digitsOptional.ifPresent(digits -> {
            final var annotation = new AnnotationExpression(constraintsPackage + ".Digits")
                    .argument("integer", LiteralExpression.createIntLiteralExpression(digits.integer()))
                    .argument("fraction", LiteralExpression.createIntLiteralExpression(digits.fraction()));
            field.annotation(annotation);
        });
    }

    protected boolean supportsDigits(final TypeMirror type) {
        return digitsTypes.contains(type) || types.isAssignable(charSequenceType, type);
    }
    private void addPastConstraint(final OpenApiSchema<?> schema,
                                   final VariableDeclaration<?> field) {
        final var allowedValue = ValidationExtensions.allowedValue(schema.extensions());

        if ("past".equals(allowedValue)
                && isFutureSupported(field.getVarType().getType())) {
            field.annotation(new AnnotationExpression(constraintsPackage + ".Past"));
        }
    }

    private void addPastOrPresentConstraint(final OpenApiSchema<?> schema,
                                            final VariableDeclaration<?> field) {
        final var allowedValue = ValidationExtensions.allowedValue(schema.extensions());

        if ("pastOrPresent".equals(allowedValue)
                && isFutureSupported(field.getVarType().getType())) {
            field.annotation(new AnnotationExpression(constraintsPackage + ".PastOrPresent"));
        }
    }

    private void addFutureOrPresentConstraint(final OpenApiSchema<?> schema,
                                              final VariableDeclaration<?> field) {
        final var allowedValue = ValidationExtensions.allowedValue(schema.extensions());

        if ("future".equals(allowedValue)
                && isFutureSupported(field.getVarType().getType())) {
            field.annotation(new AnnotationExpression(constraintsPackage + ".addFutureOrPresent"));
        }
    }

    private void addFutureConstraint(final OpenApiSchema<?> schema,
                                     final VariableDeclaration<?> field) {
        final var allowedValue = ValidationExtensions.allowedValue(schema.extensions());
        if ("future".equals(allowedValue)
                && isFutureSupported(field.getVarType().getType())) {
            field.annotation(new AnnotationExpression(constraintsPackage + ".Future"));
        }
    }

    private void addNullConstraint(final OpenApiSchema<?> schema,
                                   final VariableDeclaration<?> field) {
        if (schema.extensions().containsKey(Extensions.ASSERT)
                && schema.extensions().get(Extensions.ASSERT) == null) {
            field.annotation(constraintsPackage + ".Null");
        }
    }

    private void addNotNullConstraint(final OpenApiSchema<?> schema,
                                      final VariableDeclaration<?> field) {
        if (Boolean.FALSE.equals(schema.nullable())) {
            field.annotation(constraintsPackage + ".NotNull");
        }
    }

    protected boolean isFutureSupported(final TypeMirror type) {
        return this.futureTypes.contains(type);
    }

    private void addNegativeConstraint(final OpenApiSchema<?> schema,
                                       final VariableDeclaration<?> field) {
        if ("negative".equals(schema.extensions().get(Extensions.ASSERT))) {
            field.annotation(constraintsPackage + ".Negative");
        }
    }

    private void addNegativeOrZeroConstraint(final OpenApiSchema<?> schema,
                                             final VariableDeclaration<?> field) {
        if ("negativeOrZero".equals(schema.extensions().get(Extensions.ASSERT))) {
            field.annotation(constraintsPackage + ".NegativeOrZero");
        }
    }

    private void addPositiveConstraint(final OpenApiSchema<?> schema,
                                       final VariableDeclaration<?> field) {
        if ("positive".equals(schema.extensions().get(Extensions.ASSERT))) {
            field.annotation(constraintsPackage + ".Positive");
        }
    }

    private void addPositiveOrZeroConstraint(final OpenApiSchema<?> schema,
                                             final VariableDeclaration<?> field) {
        if ("positiveOrZero".equals(schema.extensions().get(Extensions.ASSERT))) {
            field.annotation(constraintsPackage + ".PositiveOrZero");
        }
    }
}

