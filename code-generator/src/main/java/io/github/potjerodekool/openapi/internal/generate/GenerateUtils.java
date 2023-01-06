package io.github.potjerodekool.openapi.internal.generate;

import io.github.potjerodekool.openapi.internal.ast.Attribute;
import io.github.potjerodekool.openapi.internal.ast.element.AnnotationValue;
import io.github.potjerodekool.openapi.internal.ast.element.ExecutableElement;
import io.github.potjerodekool.openapi.internal.ast.element.MethodElement;
import io.github.potjerodekool.openapi.internal.ast.expression.ArrayInitializerExpression;
import io.github.potjerodekool.openapi.internal.ast.expression.Expression;
import io.github.potjerodekool.openapi.internal.ast.type.DeclaredType;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import io.github.potjerodekool.openapi.internal.ast.type.java.WildcardType;
import io.github.potjerodekool.openapi.internal.ast.util.TypeUtils;
import io.github.potjerodekool.openapi.internal.util.GenerateException;
import io.github.potjerodekool.openapi.internal.util.Utils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GenerateUtils {

    private final TypeUtils typeUtils;

    public GenerateUtils(final TypeUtils typeUtils) {
        this.typeUtils = typeUtils;
    }

    public Attribute.Compound createAnnotation(final String name,
                                               final String memberName,
                                               final Attribute.Compound value) {
        return createAnnotation(
                name,
                new AnnotationMember(
                        memberName,
                        value
                )
        );
    }

    public Attribute.Compound createAnnotation(final String name,
                                               final AnnotationMember member) {
        return createAnnotation(name, List.of(member));
    }

    public Attribute.Compound createAnnotation(final String name,
                                               final List<AnnotationMember> members) {
        return Attribute.compound(name, toMap(members));
    }

    public Attribute.Array createArrayInitializerExprOfAnnotations(final List<Attribute.Compound> list) {
        final var values = list.toArray(Attribute[]::new);
        return Attribute.array(values);
    }

    public Attribute.Array createArrayInitializerExprOfStrings(final List<@NonNull String> list) {
        final var attributes = list.stream()
                .map(Attribute::constant)
                .toList();
        return Attribute.array(attributes);
    }

    public <E extends Expression> ArrayInitializerExpression createArrayInitializerExpr(final List<@NonNull E> list) {
        return new ArrayInitializerExpression(
                list.stream()
                .map(it -> (Expression) it)
                .toList());
    }

    public Type<?> getFirstTypeArg(final Type<?> type) {
        if (type.isDeclaredType()) {
            final var declaredType = (DeclaredType) type;
            final var typeArgumentOptional = declaredType.getTypeArguments();

            if (typeArgumentOptional.isEmpty()) {
                throw new GenerateException("Expected a type argument");
            }

            final var typeArguments = typeArgumentOptional.get();

            if (typeArguments.isEmpty()) {
                throw new GenerateException("Expected a type argument");
            }
            return typeArguments.get(0);
        } else {
            return type;
        }
    }

    public Attribute createArraySchemaAnnotation(final Type<?> elementType) {
        return createAnnotation("io.swagger.v3.oas.annotations.media.ArraySchema", "schema",
                createSchemaAnnotation(elementType, false)
        );
    }

    public Attribute.Compound createSchemaAnnotation(final Type<?> type,
                                                     final Boolean required) {
        final var members = new ArrayList<AnnotationMember>();

        final Type<?> implementationType;

        if (type.isWildCardType()) {
            final var wt = (WildcardType) type;
            if (wt.getExtendsBound().isPresent()) {
                implementationType = wt.getExtendsBound().get();
            } else if (wt.getSuperBound().isPresent()) {
                implementationType = wt.getSuperBound().get();
            } else {
                //Will result in compilation error in generated code.
                implementationType = wt;
            }
        } else if (typeUtils.isMapType(type)) {
            implementationType = typeUtils.createMapType();
        } else {
            implementationType = type;
        }

        members.add(new AnnotationMember("implementation",
                Attribute.clazz(toNonWildCardType(implementationType))));

        if (Utils.isTrue(required)) {
            members.add(new AnnotationMember("required", Attribute.constant(true)));
        }

        return Attribute.compound(
                "io.swagger.v3.oas.annotations.media.Schema",
                toMap(members)
        );
    }

    private Type<?> toNonWildCardType(final Type<?> type) {
        if (type.isWildCardType()) {
            final var wildcardType = (WildcardType) type;
            final var extendsBoundOptional = wildcardType.getExtendsBound();
            final var superBoundOptional = wildcardType.getSuperBound();
            if (extendsBoundOptional.isPresent()) {
                return extendsBoundOptional.get();
            } else if (superBoundOptional.isPresent()) {
                return superBoundOptional.get();
            } else {
                throw new UnsupportedOperationException("No bounds");
            }
        } else {
            return type;
        }
    }

    public Attribute.Compound createSchemaAnnotation(final String type,
                                                     final @Nullable String format) {
        final var members = new ArrayList<AnnotationMember>();
        members.add(new AnnotationMember("type", Attribute.constant(type)));

        if (format != null) {
            members.add(new AnnotationMember("format", Attribute.constant(format)));
        }

        return Attribute.compound(
                "io.swagger.v3.oas.annotations.media.Schema",
                toMap(members)
        );
    }

    private Map<ExecutableElement, AnnotationValue> toMap(final List<AnnotationMember> list) {
        return list.stream()
                .collect(Collectors.toMap(
                        it -> MethodElement.createMethod(it.name()),
                        AnnotationMember::value
                ));
    }

}
