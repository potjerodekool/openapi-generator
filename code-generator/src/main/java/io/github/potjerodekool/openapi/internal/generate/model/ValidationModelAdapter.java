package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.RequestCycleLocation;
import io.github.potjerodekool.openapi.generate.model.ModelAdapter;
import io.github.potjerodekool.openapi.internal.ast.TypeUtils;
import io.github.potjerodekool.openapi.internal.ast.element.MethodElement;
import io.github.potjerodekool.openapi.internal.ast.element.VariableElement;
import io.github.potjerodekool.openapi.internal.ast.expression.AnnotationExpression;
import io.github.potjerodekool.openapi.internal.ast.expression.Expression;
import io.github.potjerodekool.openapi.internal.ast.expression.LiteralExpression;
import io.github.potjerodekool.openapi.internal.ast.type.DeclaredType;
import io.github.potjerodekool.openapi.internal.ast.type.PrimitiveType;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import io.github.potjerodekool.openapi.internal.di.Bean;
import io.github.potjerodekool.openapi.internal.di.ConditionalOnMissingBean;
import io.github.potjerodekool.openapi.internal.util.SetBuilder;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;
import jakarta.inject.Inject;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.*;

@Bean
@ConditionalOnMissingBean
public class ValidationModelAdapter implements ModelAdapter {

    private final Set<Type<?>> minMaxTypes;

    private final Type<?> charSequenceType;

    private final Set<Type<?>> digitsTypes;

    private final Set<Type<?>> sizeTypes;

    private final Set<Type<?>> futureTypes;

    private final String notNullAnnotationClassName;
    private final String validAnnotationClassName;
    private final String constraintsPackage;

    private final TypeUtils typeUtils;

    @Inject
    public ValidationModelAdapter(final OpenApiGeneratorConfig config,
                                  final TypeUtils typeUtils) {
        final var validationBasePackage = (config.isFeatureEnabled(OpenApiGeneratorConfig.FEATURE_JAKARTA_VALIDATION) ? "jakarta" : "javax") + ".validation";
        this.constraintsPackage = validationBasePackage + ".constraints";
        this.notNullAnnotationClassName = constraintsPackage + ".NotNull";
        this.validAnnotationClassName = validationBasePackage + ".Valid";
        this.typeUtils = typeUtils;
        this.charSequenceType = typeUtils.createCharSequenceType();
        this.minMaxTypes = initMinMaxTypes(typeUtils);
        this.sizeTypes = initSizeTypes(typeUtils);
        this.digitsTypes = initDigitsTypes(typeUtils);
        this.futureTypes = initFutureTypes(typeUtils);
    }

    public TypeUtils getTypeUtils() {
        return typeUtils;
    }

    private static Set<Type<?>> initMinMaxTypes(final TypeUtils types) {
        return Set.of(
                types.createDeclaredType("java.math.BigInteger"),
                PrimitiveType.BYTE,
                types.createDeclaredType("java.lang.Byte"),
                PrimitiveType.SHORT,
                types.createDeclaredType("java.lang.Short"),
                PrimitiveType.INT,
                types.createDeclaredType("java.lang.Integer"),
                PrimitiveType.LONG,
                types.createDeclaredType("java.lang.Long")
        );
    }

    private static Set<Type<?>> initSizeTypes(final TypeUtils typeUtils) {
        return Set.of(
                typeUtils.createListType(),
                typeUtils.createMapType()
        );
    }

    private static Set<Type<?>> initDigitsTypes(final TypeUtils typeUtils) {
        return Set.of(
                typeUtils.createDeclaredType("java.math.BigDecimal"),
                typeUtils.createDeclaredType("java.math.BigInteger"),
                PrimitiveType.BYTE,
                typeUtils.createDeclaredType("java.lang.Byte"),
                PrimitiveType.SHORT,
                typeUtils.createDeclaredType("java.lang.Short"),
                PrimitiveType.INT,
                typeUtils.createDeclaredType("java.lang.Integer"),
                PrimitiveType.LONG,
                typeUtils.createDeclaredType("java.lang.Long")
        );
    }

    private static Set<Type<?>> initFutureTypes(final TypeUtils typeUtils) {
        return new SetBuilder<Type<?>>()
                .add(typeUtils.createDeclaredType("java.util.Date"))
                .add(typeUtils.createDeclaredType("java.util.Calendar"))
                .add(typeUtils.createDeclaredType("java.time.Instant"))
                .add(typeUtils.createDeclaredType("java.time.LocalDate"))
                .add(typeUtils.createDeclaredType("java.time.LocalDateTime"))
                .add(typeUtils.createDeclaredType("java.time.MonthDay"))
                .add(typeUtils.createDeclaredType("java.time.OffsetDateTime"))
                .add(typeUtils.createDeclaredType("java.time.OffsetTime"))
                .add(typeUtils.createDeclaredType("java.time.Year"))
                .add(typeUtils.createDeclaredType("java.time.YearMonth"))
                .add(typeUtils.createDeclaredType("java.time.ZonedDateTime"))
                .add(typeUtils.createDeclaredType("java.time.chrono.HijrahDate"))
                .add(typeUtils.createDeclaredType("java.time.chrono.JapaneseDate"))
                .add(typeUtils.createDeclaredType("java.time.chrono.JapaneseDate"))
                .add(typeUtils.createDeclaredType("java.time.chrono.MinguoDate"))
                .add(typeUtils.createDeclaredType("java.time.chrono.ThaiBuddhistDate"))
                .build();
    }

    @Override
    public void adaptField(final HttpMethod httpMethod,
                           final RequestCycleLocation requestCycleLocation,
                           final OpenApiProperty property,
                           final VariableElement fieldDeclaration) {
        if (requestCycleLocation == RequestCycleLocation.REQUEST) {
            addMinAndMax(property, fieldDeclaration);
            addSize(property, fieldDeclaration);
            addPattern(property, fieldDeclaration);
            addEmail(property, fieldDeclaration);
            addAssertTrueOrFalse(property, fieldDeclaration);
            addDigits(property, fieldDeclaration);
            addPast(property, fieldDeclaration);
            addPastOrPresent(property, fieldDeclaration);
            addFutureOrPresent(property, fieldDeclaration);
            addFuture(property, fieldDeclaration);
        }
    }

    private void addMinAndMax(final OpenApiProperty property,
                              final VariableElement fieldDeclaration) {
        final var fieldType = fieldDeclaration.getType();

        if (!minMaxTypes.contains(fieldType)) {
            return;
        }

        final var constraints = property.constraints();

        final var minimum = incrementAndToString(constraints.minimum(), constraints.exclusiveMinimum());

        if (minimum != null) {
            fieldDeclaration.addAnnotation(new AnnotationExpression(
                    constraintsPackage + ".Min",
                    LiteralExpression.createLongLiteralExpression(minimum)
            ));
        }

        final var maximum = decrementAndToString(constraints.maximum(), constraints.exclusiveMaximum());

        if (maximum != null) {
            fieldDeclaration.addAnnotation(
                    new AnnotationExpression(
                            constraintsPackage + ".Max",
                            LiteralExpression.createLongLiteralExpression(maximum)
                    )
            );
        }
    }

    private void addSize(final OpenApiProperty property,
                         final VariableElement fieldDeclaration) {
        final var fieldType = fieldDeclaration.getType();

        if (!isCharSequenceField(fieldDeclaration) &&
                !sizeTypes.contains(fieldType)) {
            return;
        }

        final Integer min;
        final Integer max;

        final var constrains = property.constraints();

        if (typeUtils.isStringType(fieldType)) {
            min = constrains.minLength();
            max = constrains.maxLength();
        } else if (typeUtils.isListType(fieldType) || fieldType.isArrayType()) {
            min = constrains.minItems();
            max = constrains.maxItems();
        } else {
            min = null;
            max = null;
        }

        if (min != null || max != null) {
            final var members = new HashMap<String, Expression>();

            if (min != null && min > 0) {
                members.put("min", LiteralExpression.createIntLiteralExpression(Integer.toString(min)));
            }

            if (max != null && max < Integer.MAX_VALUE) {
                members.put("max", LiteralExpression.createIntLiteralExpression(Integer.toString(max)));
            }

            fieldDeclaration.addAnnotation(new AnnotationExpression(
                    constraintsPackage + ".Size",
                    members
            ));
        }
    }

    private void addPattern(final OpenApiProperty property,
                            final VariableElement fieldDeclaration) {
        if (!isCharSequenceField(fieldDeclaration)) {
            return;
        }

        final var constrains = property.constraints();

        final var pattern = constrains.pattern();

        if (pattern != null) {
            fieldDeclaration.addAnnotation(new AnnotationExpression(
                    constraintsPackage + ".Pattern",
                    Map.of("regexp", LiteralExpression.createStringLiteralExpression(pattern))
            ));
        }
    }

    private void addEmail(final OpenApiProperty property,
                          final VariableElement fieldDeclaration) {
        if (!isCharSequenceField(fieldDeclaration)) {
            return;
        }

        final var propertyType = property.type();
        final var constrains = property.constraints();

        if ("email".equals(propertyType.format())) {
            final var pattern = constrains.pattern();
            final var members = new HashMap<String, Expression>();

            if (pattern != null) {
                members.put("regexp", LiteralExpression.createStringLiteralExpression(pattern));
            }

            final var annotation = new AnnotationExpression(constraintsPackage + ".Email", members);

            fieldDeclaration.addAnnotation(annotation);
        }
    }
    private @Nullable String incrementAndToString(final @Nullable Number number,
                                                  final @Nullable Boolean exclusiveMinimum) {
        if (number == null) {
            return null;
        } else if (Boolean.TRUE.equals(exclusiveMinimum)) {
            return Long.valueOf(number instanceof Integer i ? i + 1 : ((Long)number) + 1L).toString();
        } else {
            return number.toString();
        }
    }

    private @Nullable String decrementAndToString(final @Nullable Number number,
                                                  final @Nullable Boolean exclusiveMaximum) {
        if (number == null) {
            return null;
        } else if (Boolean.TRUE.equals(exclusiveMaximum)) {
            return Long.valueOf(number instanceof Integer i ? i - 1 : ((Long)number) - 1L).toString();
        } else {
            return number.toString();
        }
    }


    @Override
    public void adaptGetter(final HttpMethod httpMethod,
                            final RequestCycleLocation requestCycleLocation,
                            final OpenApiProperty property,
                            final MethodElement method) {
        final var returnType = method.getReturnType();
        final var isPrimitiveType = returnType.isPrimitiveType();
        final var isListType = typeUtils.isListType(returnType);

        if (!isPrimitiveType && !isListType) {
            final var ct = (DeclaredType) returnType;
            ct.addAnnotation(new AnnotationExpression(notNullAnnotationClassName));
        }

        if (requestCycleLocation == RequestCycleLocation.REQUEST && property.type().format() != null) {
            method.addAnnotation(validAnnotationClassName);
        }
    }

    private boolean isCharSequenceField(final VariableElement fieldDeclaration) {
        final var fieldType = fieldDeclaration.getType();
        return charSequenceType.isAssignableBy(fieldType);
    }


    private void addAssertTrueOrFalse(final OpenApiProperty property,
                                      final VariableElement fieldDeclaration) {
        final var fieldType = fieldDeclaration.getType();
        if (typeUtils.isBooleanType(fieldType)) {
            if (Boolean.TRUE.equals(property.constraints().allowedValue())) {
                fieldDeclaration.addAnnotation(constraintsPackage + ".AssertTrue");
            } else if (Boolean.FALSE.equals(property.constraints().allowedValue())) {
                fieldDeclaration.addAnnotation(constraintsPackage + ".AssertFalse");
            }
        }
    }

    private void addDigits(final OpenApiProperty property,
                           final VariableElement field) {
        final var digits = property.constraints().digits();

        if (digits != null && supportsDigits(field.getType())) {
            final var annotation = new AnnotationExpression(
                    constraintsPackage + ".Digits",
                    Map.of(
                            "integer",
                            LiteralExpression.createIntLiteralExpression(Integer.toString(digits.integer())),
                            "fraction",
                            LiteralExpression.createIntLiteralExpression(Integer.toString(digits.fraction()))
                    )
            );

            field.addAnnotation(annotation);
        }
    }

    protected boolean supportsDigits(final Type<?> type) {
        return digitsTypes.contains(type) || charSequenceType.isAssignableBy(
                type
        );
    }
    private void addPast(final OpenApiProperty property,
                                  final VariableElement field) {
        if ("past".equals(property.constraints().allowedValue())
                && isFutureSupported(field.getType())) {
            field.addAnnotation(constraintsPackage + ".Past");
        }
    }

    private void addPastOrPresent(final OpenApiProperty property,
                                  final VariableElement field) {
        if ("pastOrPresent".equals(property.constraints().allowedValue())
                && isFutureSupported(field.getType())) {
            field.addAnnotation(constraintsPackage + ".PastOrPresent");
        }
    }

    private void addFutureOrPresent(final OpenApiProperty property,
                                    final VariableElement field) {
        if ("future".equals(property.constraints().allowedValue())
                && isFutureSupported(field.getType())) {
            field.addAnnotation(constraintsPackage + ".addFutureOrPresent");
        }
    }

    private void addFuture(final OpenApiProperty property,
                           final VariableElement field) {
        if ("future".equals(property.constraints().allowedValue())
                && isFutureSupported(field.getType())) {
            field.addAnnotation(constraintsPackage + ".Future");
        }
    }

    protected boolean isFutureSupported(final Type<?> type) {
        return this.futureTypes.contains(type);
    }

}