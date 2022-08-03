package io.github.potjerodekool.openapi.generate;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import io.github.potjerodekool.openapi.util.NodeListCollectors;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public final class GenerateHelper {

    private GenerateHelper() {
    }

    public static AnnotationExpr createAnnotation(final String name,
                                                  final String memberName,
                                                  final boolean value) {
        return createAnnotation(
                name,
                new AnnotationMember(
                        memberName,
                        value
                )
        );
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

    public static AnnotationExpr createAnnotation(final String name,
                                                  final String memberName,
                                                  final ClassExpr value) {
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
                                                        final AnnotationMember member1,
                                                        final AnnotationMember member2) {
        return createAnnotation(name, List.of(member1, member2));
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

    private static LiteralExpr createLiteralExpression(final int value) {
        return new IntegerLiteralExpr(Integer.toString(value));
    }

    private static LiteralExpr createLiteralExpression(final boolean value) {
        return new BooleanLiteralExpr(value);
    }

    private static LiteralExpr createLiteralExpression(final String value) {
        return new StringLiteralExpr(value);
    }

    public static @Nullable Expression getDefaultValue(final Type type) {
        if (type.isPrimitiveType()) {
            final var primitiveType = ((PrimitiveType) type);

            return switch (primitiveType.getType()) {
                case BOOLEAN -> new BooleanLiteralExpr();
                case CHAR -> new CharLiteralExpr();
                case BYTE, SHORT, INT -> new IntegerLiteralExpr();
                case LONG -> new LongLiteralExpr();
                case FLOAT -> new DoubleLiteralExpr("0F");
                case DOUBLE -> new DoubleLiteralExpr("0D");
            };
        } else if (type.isClassOrInterfaceType()) {
            return new NullLiteralExpr();
        } else if (isListType(type)) {
            return new MethodCallExpr()
                    .setScope(new NameExpr(Constants.LIST_CLASS_NAME))
                    .setName("of");
        } else {
            return null;
        }
    }

    public static boolean isListType(final Type type) {
        if (!type.isClassOrInterfaceType()) {
            return false;
        } else {
            final var declaredType = (ClassOrInterfaceType) type;
            return declaredType.getNameWithScope().equals(Constants.LIST_CLASS_NAME);
        }
    }
}
