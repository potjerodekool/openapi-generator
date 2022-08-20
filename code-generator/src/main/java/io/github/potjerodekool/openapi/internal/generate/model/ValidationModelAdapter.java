package io.github.potjerodekool.openapi.internal.generate.model;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.resolution.types.ResolvedType;
import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.RequestCycleLocation;
import io.github.potjerodekool.openapi.generate.model.ModelAdapter;
import io.github.potjerodekool.openapi.internal.generate.GenerateUtils;
import io.github.potjerodekool.openapi.internal.generate.Types;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashSet;
import java.util.Set;

public class ValidationModelAdapter implements ModelAdapter {

    private final Set<Type> minMaxTypes = new HashSet<>();

    private final ResolvedType charSequenceType;

    private final Set<Type> digitsTypes = new HashSet<>();

    private final Set<Type> sizeTypes = new HashSet<>();

    private final String notNullAnnotationClassName;
    private final String validAnnotationClassName;

    private final String validationBasePackage;

    private final String constraintsPackage;

    private final Types types;

    private final GenerateUtils generateUtils;

    public ValidationModelAdapter(final OpenApiGeneratorConfig config,
                                  final Types types,
                                  final GenerateUtils generateUtils) {
        this.validationBasePackage = config.isFeatureEnabled(OpenApiGeneratorConfig.FEATURE_JAKARTA_VALIDATION) ? "jakarta" : "javax";
        this.constraintsPackage = validationBasePackage + ".validation.constraints";
        this.notNullAnnotationClassName = constraintsPackage + ".NotNull";
        this.validAnnotationClassName = constraintsPackage + ".Valid";
        this.types = types;
        this.generateUtils = generateUtils;
        this.charSequenceType = types.createCharSequenceType().resolve();
        initMinMaxTypes();
        initSizeTypes();
        initDigitsTypes();
    }

    private void initMinMaxTypes() {
        minMaxTypes.add(types.createType("java.math.BigInteger"));
        minMaxTypes.add(PrimitiveType.byteType());
        minMaxTypes.add(types.createType("java.lang.Byte"));
        minMaxTypes.add(PrimitiveType.shortType());
        minMaxTypes.add(types.createType("java.lang.Short"));
        minMaxTypes.add(PrimitiveType.intType());
        minMaxTypes.add(types.createType("java.lang.Integer"));
        minMaxTypes.add(PrimitiveType.longType());
        minMaxTypes.add(types.createType("java.lang.Long"));
    }

    private void initSizeTypes() {
        sizeTypes.add(types.createListType());
        sizeTypes.add(types.createMapType());
    }

    private void initDigitsTypes() {
        digitsTypes.add(types.createType("java.math.BigDecimal"));
        digitsTypes.add(types.createType("java.math.BigInteger"));
        digitsTypes.add(PrimitiveType.byteType());
        digitsTypes.add(types.createType("java.lang.Byte"));
        digitsTypes.add(PrimitiveType.shortType());
        digitsTypes.add(types.createType("java.lang.Short"));
        digitsTypes.add(PrimitiveType.intType());
        digitsTypes.add(types.createType("java.lang.Integer"));
        digitsTypes.add(PrimitiveType.longType());
        digitsTypes.add(types.createType("java.lang.Long"));
    }


    @Override
    public void adaptField(final HttpMethod httpMethod,
                           final RequestCycleLocation requestCycleLocation,
                           final OpenApiProperty property,
                           final FieldDeclaration fieldDeclaration) {
        addMinAndMax(property, fieldDeclaration);
        addSize(property, fieldDeclaration);
        addPattern(property, fieldDeclaration);
        addEmail(property, fieldDeclaration);
        addAssertTrueOrFalse(property, fieldDeclaration);
        addDigits(property, fieldDeclaration);
    }

    private void addMinAndMax(final OpenApiProperty property,
                              final FieldDeclaration fieldDeclaration) {
        final var fieldType = generateUtils.getFieldType(fieldDeclaration);

        if (!minMaxTypes.contains(fieldType)) {
            return;
        }

        final var constraints = property.constraints();

        final var minimum = incrementAndToString(constraints.minimum(), constraints.exclusiveMinimum());

        if (minimum != null) {
            fieldDeclaration.addSingleMemberAnnotation(validationBasePackage + ".validation.constraint.Min", new LongLiteralExpr(minimum));
        }

        final var maximum = decrementAndToString(constraints.maximum(), constraints.exclusiveMaximum());

        if (maximum != null) {
            fieldDeclaration.addSingleMemberAnnotation(validationBasePackage + ".validation.constraint.Max", new LongLiteralExpr(maximum));
        }
    }

    private void addSize(final OpenApiProperty property,
                         final FieldDeclaration fieldDeclaration) {
        final var fieldType = generateUtils.getFieldType(fieldDeclaration);

        if (!isCharSequenceField(fieldDeclaration) &&
                !sizeTypes.contains(fieldType)) {
            return;
        }

        final Integer min;
        final Integer max;

        final var constrains = property.constraints();

        if (types.isStringType(fieldType)) {
            min = constrains.minLength();
            max = constrains.maxLength();
        } else if (types.isListType(fieldType) || fieldType.isArrayType()) {
            min = constrains.minItems();
            max = constrains.maxItems();
        } else {
            min = null;
            max = null;
        }

        if (min != null || max != null) {
            final var memberValuePairs = new NodeList<MemberValuePair>();

            if (min != null && min > 0) {
                memberValuePairs.add(new MemberValuePair("min", new IntegerLiteralExpr(Integer.toString(min))));
            }

            if (max != null && max < Integer.MAX_VALUE) {
                memberValuePairs.add(new MemberValuePair("max", new IntegerLiteralExpr(Integer.toString(max))));
            }

            fieldDeclaration.addAnnotation(new NormalAnnotationExpr(new Name(validationBasePackage + ".validation.constraint.Size"), memberValuePairs));
        }
    }

    private void addPattern(final OpenApiProperty property,
                            final FieldDeclaration fieldDeclaration) {
        if (!isCharSequenceField(fieldDeclaration)) {
            return;
        }

        final var constrains = property.constraints();

        final var pattern = constrains.pattern();

        if (pattern != null) {
            fieldDeclaration.addAnnotation(
                    new NormalAnnotationExpr(
                            new Name(validationBasePackage + ".validation.constraint.Pattern"),
                            NodeList.nodeList(
                                    new MemberValuePair("regexp", new StringLiteralExpr(pattern))
                            )
                    )
            );
        }
    }

    private void addEmail(final OpenApiProperty property,
                          final FieldDeclaration fieldDeclaration) {
        if (!isCharSequenceField(fieldDeclaration)) {
            return;
        }

        final var propertyType = property.type();
        final var constrains = property.constraints();

        if ("email".equals(propertyType.format())) {
            final var pattern = constrains.pattern();
            final var memberValuePairList = new NodeList<MemberValuePair>();

            if (pattern != null) {
                memberValuePairList.add(new MemberValuePair("regexp", new StringLiteralExpr(pattern)));
            }

            new NormalAnnotationExpr(
                    new Name(validationBasePackage + ".validation.constraint.Email"),
                    memberValuePairList
            );

            fieldDeclaration.addMarkerAnnotation(validationBasePackage + ".validation.constraint.Email");
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
                            final MethodDeclaration methodDeclaration) {
        final var returnType = methodDeclaration.getType();
        final var isPrimitiveType = returnType.isPrimitiveType();
        final var isListType = generateUtils.isListType(returnType);

        if (!isPrimitiveType && !isListType) {
            final var ct = (ClassOrInterfaceType) returnType;
            ct.addMarkerAnnotation(notNullAnnotationClassName);
        }

        if (requestCycleLocation == RequestCycleLocation.REQUEST && property.type().format() != null) {
            methodDeclaration.addAnnotation(validAnnotationClassName);
        }
    }

    private boolean isCharSequenceField(final FieldDeclaration fieldDeclaration) {
        final var fieldType = generateUtils.getFieldType(fieldDeclaration);
        return charSequenceType.isAssignableBy(fieldType.resolve());
    }


    private void addAssertTrueOrFalse(final OpenApiProperty property,
                                      final FieldDeclaration fieldDeclaration) {
        final var fieldType = generateUtils.getFieldType(fieldDeclaration);
        if (types.isBooleanType(fieldType)) {
            if (Boolean.TRUE.equals(property.constraints().allowedValue())) {
                fieldDeclaration.addAnnotation(
                    new MarkerAnnotationExpr(constraintsPackage + ".AssertTrue")
                );
            } else if (Boolean.FALSE.equals(property.constraints().allowedValue())) {
                fieldDeclaration.addAnnotation(
                        new MarkerAnnotationExpr(constraintsPackage + ".AssertFalse")
                );
            }
        }
    }

    private void addDigits(final OpenApiProperty property,
                           final FieldDeclaration fieldDeclaration) {
        final var digits = property.constraints().digits();

        if (digits != null && supportsDigits(generateUtils.getFieldType(fieldDeclaration))) {
            final var annotation = new NormalAnnotationExpr(
                    new Name(constraintsPackage + ".Digits"),
                    NodeList.nodeList(
                            new MemberValuePair("integer", new IntegerLiteralExpr(
                                    Integer.toString(digits.integer())
                            )),
                            new MemberValuePair("fraction", new IntegerLiteralExpr(
                                    Integer.toString(digits.fraction())
                            ))
                    )
            );

            fieldDeclaration.addAnnotation(annotation);
        }
    }

    protected boolean supportsDigits(final Type type) {
        try {
            return digitsTypes.contains(type) || charSequenceType.isAssignableBy(
                    type.resolve()
            );
        } catch (final Exception e) {
            return false;
        }
    }

}
