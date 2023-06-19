package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.MethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.expression.Expression;
import io.github.potjerodekool.codegen.model.tree.expression.LiteralExpression;
import io.github.potjerodekool.codegen.model.tree.expression.NameExpression;
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ParameterizedType;
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
import io.github.potjerodekool.openapi.internal.util.SetBuilder;
import io.github.potjerodekool.openapi.internal.util.TypeUtils;
import io.github.potjerodekool.openapi.tree.Constraints;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;
import jakarta.inject.Inject;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static io.github.potjerodekool.codegen.model.element.Name.getQualifiedNameOf;

@Bean
@ConditionalOnMissingBean
//https://jakarta.ee/specifications/bean-validation/3.0/jakarta-bean-validation-spec-3.0.html
public class ValidationModelAdapter implements ModelAdapter {

    private static final String LIST_CLASS_NAME = "java.util.List";
    private static final String MAP_CLASS_NAME = "java.util.Map";

    private final Set<TypeMirror> minMaxTypes;

    private final TypeMirror charSequenceType;

    private final Set<TypeMirror> digitsTypes;

    private final Set<TypeMirror> sizeTypes;

    private final Set<TypeMirror> futureTypes;

    private final String validAnnotationClassName;
    private final String constraintsPackage;

    protected final TypeUtils typeUtils;
    private final Elements elements;
    private final Types types;

    @Inject
    public ValidationModelAdapter(final GeneratorConfig generatorConfig,
                                  final Environment environment,
                                  final TypeUtils typeUtils) {
        final var validationBasePackage = (generatorConfig.isFeatureEnabled(Features.FEATURE_JAKARTA) ? "jakarta" : "javax") + ".validation";
        this.constraintsPackage = validationBasePackage + ".constraints";
        this.validAnnotationClassName = validationBasePackage + ".Valid";
        this.typeUtils = typeUtils;
        this.elements = environment.getElementUtils();
        this.types = environment.getTypes();
        this.charSequenceType = typeUtils.getCharSequenceType();

        final var types = environment.getTypes();
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
        return new SetBuilder<TypeMirror>()
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
                .build();
    }

    @Override
    public void adaptField(final OpenApiProperty property,
                           final VariableDeclaration fieldDeclaration) {
        final var symbol = fieldDeclaration.getSymbol();
        if (!symbol.getCompleter().isComplete()) {
            symbol.getCompleter().complete(symbol);
        }

        addMinAndMaxConstraint(property, fieldDeclaration);
        addSizeConstraint(property, fieldDeclaration);
        addPatternConstraint(property, fieldDeclaration);
        addEmailConstraint(property, fieldDeclaration);
        addAssertTrueOrFalseConstraint(property, fieldDeclaration);
        addDigitsConstraint(property, fieldDeclaration);
        addPastConstraint(property, fieldDeclaration);
        addPastOrPresentConstraint(property, fieldDeclaration);
        addFutureOrPresentConstraint(property, fieldDeclaration);
        addFutureConstraint(property, fieldDeclaration);
        addNullConstraint(property, fieldDeclaration);
        addNotNullConstraint(property, fieldDeclaration);
        addNegativeConstraint(property, fieldDeclaration);
        addNegativeOrZeroConstraint(property, fieldDeclaration);
        addPositiveConstraint(property, fieldDeclaration);
        addPositiveOrZeroConstraint(property, fieldDeclaration);
    }

    private void addMinAndMaxConstraint(final OpenApiProperty property,
                                        final VariableDeclaration fieldDeclaration) {
        final var fieldType = fieldDeclaration.getVarType().getType();

        if (!minMaxTypes.contains(fieldType)) {
            return;
        }

        final var constraints = property.constraints();

        final boolean isIntegerOrLong = constraints.minimum() != null
            ? isIntegerOrLong(constraints.minimum())
            : isIntegerOrLong(constraints.maximum());

        final var minimum = incrementAndToString(constraints.minimum(), constraints.exclusiveMinimum());

        if (minimum != null) {
            final String annotationName;

            if (isIntegerOrLong) {
                annotationName = constraintsPackage + ".Min";
            } else {
                annotationName = constraintsPackage + ".DecimalMin";
            }

            fieldDeclaration.addAnnotation(new AnnotationExpression(
                    new ParameterizedType(new NameExpression(annotationName)),
                    Map.of(
                            "value",
                            LiteralExpression.createLongLiteralExpression(minimum)
                    )
            ));
        }

        final var maximum = decrementAndToString(constraints.maximum(), constraints.exclusiveMaximum());

        if (maximum != null) {
            final String annotationName;

            if (isIntegerOrLong) {
                annotationName = constraintsPackage + ".Max";
            } else {
                annotationName = constraintsPackage + ".DecimalMax";
            }

            fieldDeclaration.addAnnotation(
                    new AnnotationExpression(
                            new ParameterizedType(new NameExpression(annotationName)),
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

    private void addSizeConstraint(final OpenApiProperty property,
                                   final VariableDeclaration fieldDeclaration) {
        final var fieldType = fieldDeclaration.getVarType();

        if (!isCharSequenceField(fieldDeclaration) &&
                !sizeTypes.contains(fieldType)) {
            return;
        }

        final Integer min;
        final Integer max;

        final var constrains = property.constraints();

        if (true) {
            throw new UnsupportedOperationException();
        }

        /*
        if (typeUtils.isStringType(fieldType)) {
            min = constrains.minLength();
            max = constrains.maxLength();
        } else if (typeUtils.isListType(fieldType)
                || fieldType instanceof ArrayType
                || typeUtils.isMapType(fieldType)) {
            min = constrains.minItems();
            max = constrains.maxItems();
        } else {
            min = null;
            max = null;
        }
        */

        if (min != null || max != null) {
            final var members = new HashMap<String, Expression>();

            if (min != null && min > 0) {
                members.put("min", LiteralExpression.createIntLiteralExpression(min.toString()));
            }

            if (max != null && max < Integer.MAX_VALUE) {
                members.put("max", LiteralExpression.createIntLiteralExpression(max.toString()));
            }

            fieldDeclaration.addAnnotation(
                    new AnnotationExpression(
                            new ParameterizedType(new NameExpression(constraintsPackage + ".Size")),
                            members
                    )
            );
        }
    }

    private void addPatternConstraint(final OpenApiProperty property,
                                      final VariableDeclaration fieldDeclaration) {
        if (!isCharSequenceField(fieldDeclaration)) {
            return;
        }

        final var constrains = property.constraints();

        final var pattern = constrains.pattern();

        if (pattern != null) {
            fieldDeclaration.addAnnotation(
                    new AnnotationExpression(
                            new ParameterizedType(new NameExpression(constraintsPackage + ".Pattern")),
                            Map.of(
                                    "regexp", LiteralExpression.createStringLiteralExpression(pattern)
                            )
                    )
            );
        }
    }

    private void addEmailConstraint(final OpenApiProperty property,
                                    final VariableDeclaration fieldDeclaration) {
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

            fieldDeclaration.addAnnotation(
                new AnnotationExpression(
                        new ParameterizedType(new NameExpression(constraintsPackage + ".Email")),
                        members
                )
            );
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
    public void adaptGetter(final OpenApiProperty property,
                            final MethodDeclaration method) {
        if (property.type().format() != null) {
            method.addAnnotation(new AnnotationExpression(new ParameterizedType(new NameExpression(validAnnotationClassName))));
        }
    }

    private boolean isCharSequenceField(final VariableDeclaration fieldDeclaration) {
        final var fieldType = fieldDeclaration.getVarType();
        return types.isAssignable(charSequenceType, fieldType.getType());
    }

    private void addAssertTrueOrFalseConstraint(final OpenApiProperty property,
                                                final VariableDeclaration fieldDeclaration) {
        final var fieldType = fieldDeclaration.getVarType().getType();
        if (isBooleanType(fieldType)) {
            final var xAssert = property.constraints().extension(Constraints.X_ASSERT);

            if (Boolean.TRUE.equals(xAssert)) {
                fieldDeclaration.addAnnotation(new AnnotationExpression(constraintsPackage + ".AssertTrue"));
            } else if (Boolean.FALSE.equals(xAssert)) {
                fieldDeclaration.addAnnotation(new AnnotationExpression(constraintsPackage + ".AssertFalse"));
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

    private void addDigitsConstraint(final OpenApiProperty property,
                                     final VariableDeclaration field) {
        final var digits = property.constraints().digits();

        if (digits != null && supportsDigits(field.getVarType().getType())) {
            final var annotation = new AnnotationExpression(
                    constraintsPackage + ".Digits",
                    Map.of(
                            "integer", LiteralExpression.createIntLiteralExpression(Integer.toString(digits.integer())),
                            "fraction", LiteralExpression.createIntLiteralExpression(Integer.toString(digits.fraction()))
                    )
            );
            field.addAnnotation(annotation);
        }
    }

    protected boolean supportsDigits(final TypeMirror type) {
        return digitsTypes.contains(type) || types.isAssignable(charSequenceType, type);
    }
    private void addPastConstraint(final OpenApiProperty property,
                                   final VariableDeclaration field) {
        if ("past".equals(property.constraints().allowedValue())
                && isFutureSupported(field.getVarType().getType())) {
            field.addAnnotation(new AnnotationExpression(
                    constraintsPackage + ".Past"
            ));
        }
    }

    private void addPastOrPresentConstraint(final OpenApiProperty property,
                                            final VariableDeclaration field) {
        if ("pastOrPresent".equals(property.constraints().allowedValue())
                && isFutureSupported(field.getVarType().getType())) {
            field.addAnnotation(new AnnotationExpression(constraintsPackage + ".PastOrPresent"));
        }
    }

    private void addFutureOrPresentConstraint(final OpenApiProperty property,
                                              final VariableDeclaration field) {
        if ("future".equals(property.constraints().allowedValue())
                && isFutureSupported(field.getVarType().getType())) {
            field.addAnnotation(new AnnotationExpression(constraintsPackage + ".addFutureOrPresent"));
        }
    }

    private void addFutureConstraint(final OpenApiProperty property,
                                     final VariableDeclaration field) {
        if ("future".equals(property.constraints().allowedValue())
                && isFutureSupported(field.getVarType().getType())) {
            field.addAnnotation(new AnnotationExpression(constraintsPackage + ".Future"));
        }
    }

    private void addNullConstraint(final OpenApiProperty property,
                                   final VariableDeclaration field) {
        if (property.constraints().hasExtension(Constraints.X_ASSERT)
                && property.constraints().extension(Constraints.X_ASSERT) == null) {
            field.addAnnotation(constraintsPackage + ".Null");
        }
    }

    private void addNotNullConstraint(final OpenApiProperty property,
                                      final VariableDeclaration field) {
        if (Boolean.FALSE.equals(property.type().nullable())) {
            field.addAnnotation(constraintsPackage + ".NotNull");
        }
    }

    protected boolean isFutureSupported(final TypeMirror type) {
        return this.futureTypes.contains(type);
    }

    private void addNegativeConstraint(final OpenApiProperty property,
                                       final VariableDeclaration field) {
        if ("negative".equals(property.constraints().extension(Constraints.X_ASSERT))) {
            field.addAnnotation(constraintsPackage + ".Negative");
        }
    }

    private void addNegativeOrZeroConstraint(final OpenApiProperty property,
                                             final VariableDeclaration field) {
        if ("negativeOrZero".equals(property.constraints().extension(Constraints.X_ASSERT))) {
            field.addAnnotation(constraintsPackage + ".NegativeOrZero");
        }
    }

    private void addPositiveConstraint(final OpenApiProperty property,
                                       final VariableDeclaration field) {
        if ("positive".equals(property.constraints().extension(Constraints.X_ASSERT))) {
            field.addAnnotation(constraintsPackage + ".Positive");
        }
    }

    private void addPositiveOrZeroConstraint(final OpenApiProperty property,
                                             final VariableDeclaration field) {
        if ("positiveOrZero".equals(property.constraints().extension(Constraints.X_ASSERT))) {
            field.addAnnotation(constraintsPackage + ".PositiveOrZero");
        }
    }
}

