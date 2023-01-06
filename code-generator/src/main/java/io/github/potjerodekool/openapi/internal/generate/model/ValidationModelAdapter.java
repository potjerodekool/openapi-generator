package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.openapi.*;
import io.github.potjerodekool.openapi.generate.model.ModelAdapter;
import io.github.potjerodekool.openapi.internal.ast.Attribute;
import io.github.potjerodekool.openapi.internal.ast.element.*;
import io.github.potjerodekool.openapi.internal.ast.util.TypeUtils;
import io.github.potjerodekool.openapi.internal.ast.type.DeclaredType;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import io.github.potjerodekool.openapi.internal.di.Bean;
import io.github.potjerodekool.openapi.internal.di.ConditionalOnMissingBean;
import io.github.potjerodekool.openapi.internal.util.SetBuilder;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;
import jakarta.inject.Inject;
import org.checkerframework.checker.nullness.qual.Nullable;

import javax.lang.model.type.TypeKind;
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
    public ValidationModelAdapter(final GeneratorConfig generatorConfig,
                                  final TypeUtils typeUtils) {
        final var validationBasePackage = (generatorConfig.isFeatureEnabled(Features.FEATURE_JAKARTA) ? "jakarta" : "javax") + ".validation";
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
                types.createPrimitiveType(TypeKind.BYTE),
                types.createDeclaredType("java.lang.Byte"),
                types.createPrimitiveType(TypeKind.SHORT),
                types.createDeclaredType("java.lang.Short"),
                types.createPrimitiveType(TypeKind.INT),
                types.createDeclaredType("java.lang.Integer"),
                types.createPrimitiveType(TypeKind.LONG),
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
                typeUtils.createPrimitiveType(TypeKind.BYTE),
                typeUtils.createDeclaredType("java.lang.Byte"),
                typeUtils.createPrimitiveType(TypeKind.SHORT),
                typeUtils.createDeclaredType("java.lang.Short"),
                typeUtils.createPrimitiveType(TypeKind.INT),
                typeUtils.createDeclaredType("java.lang.Integer"),
                typeUtils.createPrimitiveType(TypeKind.LONG),
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
            fieldDeclaration.addAnnotation(Attribute.compound(
                    constraintsPackage + ".Min",
                    Attribute.constant(Long.parseLong(minimum))
            ));
        }

        final var maximum = decrementAndToString(constraints.maximum(), constraints.exclusiveMaximum());

        if (maximum != null) {
            fieldDeclaration.addAnnotation(
                    Attribute.compound(
                            constraintsPackage + ".Max",
                            Attribute.constant(Long.parseLong(maximum))
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
            final var members = new HashMap<ExecutableElement, AnnotationValue>();

            if (min != null && min > 0) {
                members.put(MethodElement.createMethod("min"), Attribute.constant(min));
            }

            if (max != null && max < Integer.MAX_VALUE) {
                members.put(MethodElement.createMethod("max"), Attribute.constant(max));
            }

            fieldDeclaration.addAnnotation(Attribute.compound(
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
            fieldDeclaration.addAnnotation(Attribute.compound(
                    constraintsPackage + ".Pattern",
                    Map.of(MethodElement.createMethod("regexp"), Attribute.constant(pattern))
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
            final var members = new HashMap<ExecutableElement, AnnotationValue>();

            if (pattern != null) {
                members.put(MethodElement.createMethod("regexp"), Attribute.constant(pattern));
            }

            final var annotation = Attribute.compound(constraintsPackage + ".Email", members);

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

        if (!isPrimitiveType && !isListType && !returnType.isArrayType()) {
            final var ct = (DeclaredType) returnType;
            if (!ct.isNullable()) {
                ct.addAnnotation(Attribute.compound(notNullAnnotationClassName));
            }
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
            final var annotation = Attribute.compound(
                    constraintsPackage + ".Digits",
                    Map.of(
                            MethodElement.createMethod("integer"),
                            Attribute.constant(digits.integer()),
                            MethodElement.createMethod("fraction"),
                            Attribute.constant(digits.fraction())
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