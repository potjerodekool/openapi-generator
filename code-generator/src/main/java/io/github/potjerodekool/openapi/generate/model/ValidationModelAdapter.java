package io.github.potjerodekool.openapi.generate.model;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.RequestCycleLocation;
import io.github.potjerodekool.openapi.generate.GenerateUtils;
import io.github.potjerodekool.openapi.generate.Types;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;
import io.github.potjerodekool.openapi.type.OpenApiArrayType;
import org.checkerframework.checker.nullness.qual.Nullable;

public class ValidationModelAdapter implements ModelAdapter {

    private final String notNullAnnotationClassName;
    private final String validAnnotationClassName;

    private final String validationBasePackage;

    private final Types types;

    private final GenerateUtils generateUtils;

    public ValidationModelAdapter(final OpenApiGeneratorConfig config,
                                  final Types types,
                                  final GenerateUtils generateUtils) {
        this.validationBasePackage = config.isUseJakartaValidation() ? "jakarta" : "javax";
        this.notNullAnnotationClassName = validationBasePackage + ".validation.constraints.NotNull";
        this.validAnnotationClassName = validationBasePackage + ".validation.Valid";
        this.types = types;
        this.generateUtils = generateUtils;
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
    }

    private void addMinAndMax(final OpenApiProperty property,
                              final FieldDeclaration fieldDeclaration) {
        final var minimum = incrementAndToString(property.minimum(), property.exclusiveMinimum());

        if (minimum != null) {
            fieldDeclaration.addSingleMemberAnnotation(validationBasePackage + ".validation.constraint.Min", new LongLiteralExpr(minimum));
        }

        final var maximum = decrementAndToString(property.maximum(), property.exclusiveMaximum());

        if (maximum != null) {
            fieldDeclaration.addSingleMemberAnnotation(validationBasePackage + ".validation.constraint.Max", new LongLiteralExpr(maximum));
        }
    }

    private void addSize(final OpenApiProperty property,
                         final FieldDeclaration fieldDeclaration) {
        final var propertyType = property.type();

        final Integer min;
        final Integer max;

        if (types.isStringTypeName(propertyType.qualifiedName())) {
            min = property.minLength();
            max = property.maxLength();
        } else if (types.isListTypeName(propertyType.qualifiedName()) || propertyType instanceof OpenApiArrayType) {
            min = property.minItems();
            max = property.maxItems();
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
        final var pattern = property.pattern();

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
        final var propertyType = property.type();

        if (types.isStringTypeName(propertyType.qualifiedName()) && "email".equals(propertyType.format())) {
            final var pattern = property.pattern();
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

    @Override
    public void adaptSetter(final HttpMethod httpMethod,
                            final RequestCycleLocation requestCycleLocation,
                            final OpenApiProperty property,
                            final MethodDeclaration methodDeclaration) {
    }
}
