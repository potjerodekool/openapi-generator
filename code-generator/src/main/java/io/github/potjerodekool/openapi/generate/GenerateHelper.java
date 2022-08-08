package io.github.potjerodekool.openapi.generate;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import io.github.potjerodekool.openapi.util.GenerateException;
import io.github.potjerodekool.openapi.util.NodeListCollectors;
import io.github.potjerodekool.openapi.util.Utils;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.List;

public final class GenerateHelper {

    private GenerateHelper() {
    }

    public static AnnotationExpr createAnnotation(final String name,
                                                  final String memberName,
                                                  final AnnotationExpr value) {
        return createAnnotation(
                name,
                new AnnotationMember(
                        memberName,
                        value
                )
        );
    }

    public static NormalAnnotationExpr createAnnotation(final String name,
                                                        final AnnotationMember member) {
        return createAnnotation(name, List.of(member));
    }

    public static NormalAnnotationExpr createAnnotation(final String name,
                                                        final List<AnnotationMember> members) {
        final var pairs = members.stream()
                .map(member -> {
                    final var memberName = member.name();
                    final var memberValue = member.value();

                    final var expression = switch (member.type()) {
                        case ANNOTATION_EXPRESSION,
                             CLASS_EXPRESSION,
                             ARRAY_EXPRESSION -> (Expression) memberValue;
                        case STRING_LITERAL -> createLiteralExpression((String) memberValue);
                        case BOOLEAN_LITERAL -> createLiteralExpression((boolean) memberValue);
                        case INTEGER_LITERAL -> createLiteralExpression((int) memberValue);
                        //noinspection UnnecessaryDefault
                        default -> throw new IllegalArgumentException(String.format("unknown member type %s", member.type()));
                    };
                    return new MemberValuePair(memberName, expression);
                })
                .collect(NodeListCollectors.collector());
        return new NormalAnnotationExpr(new Name(name), pairs);
    }

    public static <A extends AnnotationExpr> ArrayInitializerExpr createArrayInitializerExprOfAnnotations(final List<@NonNull A> list) {
        return new ArrayInitializerExpr(list.stream().collect(NodeListCollectors.collector()));
    }

    public static ArrayInitializerExpr createArrayInitializerExprOfStrings(final List<@NonNull String> list) {
        final NodeList<Expression> nodeList = list.stream()
                .map(GenerateHelper::createLiteralExpression)
                .collect(NodeListCollectors.collector());
        return new ArrayInitializerExpr(nodeList);
    }

    public static <E extends Expression> ArrayInitializerExpr createArrayInitializerExpr(final List<@NonNull E> list) {
        return new ArrayInitializerExpr(list.stream().collect(NodeListCollectors.collector()));
    }

    public static <E extends Expression> Expression toExpression(final List<@NonNull E> list) {
        return list.size() == 1
                ? list.get(0)
                : createArrayInitializerExpr(list);
    }

    private static LiteralExpr createLiteralExpression(final int value) {
        return new IntegerLiteralExpr(Integer.toString(value));
    }

    private static LiteralExpr createLiteralExpression(final boolean value) {
        return new BooleanLiteralExpr(value);
    }

    private static LiteralExpr createLiteralExpression(final String value) {
        return new StringLiteralExpr(value);
    }

    public static Type getFirstTypeArg(final Type type) {
        if (type instanceof ClassOrInterfaceType cType) {
            return cType.getTypeArguments()
                    .filter(it -> it.size() > 0)
                    .map(it -> it.get(0))
                    .orElseThrow(() -> new GenerateException("Expected a type argument"));
        } else {
            return type;
        }
    }

    public static AnnotationExpr createArraySchemaAnnotation(final Type elementType) {
        return createAnnotation("io.swagger.v3.oas.annotations.media.ArraySchema", "schema",
                createSchemaAnnotation(elementType, false)
        );
    }

    public static AnnotationExpr createSchemaAnnotation(final Type type,
                                                         final Boolean required) {
        final var members = new NodeList<MemberValuePair>();
        members.add(new MemberValuePair("implementation", new ClassExpr(type)));

        if (Utils.isTrue(required)) {
            members.add(new MemberValuePair("required", new BooleanLiteralExpr(true)));
        }

        return new NormalAnnotationExpr(
                new Name("io.swagger.v3.oas.annotations.media.Schema"),
                members
        );
    }
}
