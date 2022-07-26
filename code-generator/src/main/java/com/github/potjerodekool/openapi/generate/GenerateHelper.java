package com.github.potjerodekool.openapi.generate;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;
import java.util.Map;

public final class GenerateHelper {

    private GenerateHelper() {
    }

    public static AnnotationExpr createAnnotation(final String name) {
        return new MarkerAnnotationExpr(new Name(name));
    }

    public static AnnotationExpr createAnnotation(final String name,
                                                  final String memberName,
                                                  final Object value) {
        return createAnnotation(name, Map.of(memberName, value));
    }

    public static AnnotationExpr createAnnotation(final String name,
                                                  final Map<String, Object> members) {
        if (members.isEmpty()) {
            return createAnnotation(name);
        }

        final var pairs = new NodeList<MemberValuePair>();

        members.entrySet().stream()
                .filter(it -> !(it.getValue() instanceof ArrayInitializerExpr aie) || aie.getValues().size() > 0)
                .map(entry -> {
                    final var memberName = entry.getKey();
                    final var memberValue = entry.getValue();
                    final Expression expression;

                    if (memberValue instanceof Expression) {
                        expression = (Expression) memberValue;
                    } else {
                        expression = createLiteralExpression(memberValue);
                    }
                    return new MemberValuePair(memberName, expression);
                }).forEach(pairs::add);

        return new NormalAnnotationExpr(new Name(name), pairs);
    }

    public static <E> ArrayInitializerExpr createArrayInitializerExpr(final List<@NonNull E> list) {
        if (list.size() == 0) {
            return new ArrayInitializerExpr();
        } else {
            final var expressions = list.stream()
                    .map(value -> {
                        if (value instanceof Expression e) {
                            return e;
                        } else {
                            return GenerateHelper.createLiteralExpression(value);
                        }
                    })
                    .toList();

            final var nodeList = new NodeList<Expression>();
            nodeList.addAll(expressions);

            return new ArrayInitializerExpr(nodeList);
        }
    }

    public static LiteralExpr createLiteralExpression(final Object value) {
        if (value == null) {
            return new NullLiteralExpr();
        } else if (value instanceof Integer i) {
            return new IntegerLiteralExpr(Integer.toString(i));
        } else if (value instanceof Boolean b) {
            return new BooleanLiteralExpr(b);
        } else if (value instanceof String s) {
            return new StringLiteralExpr(s);
        } else {
            throw new IllegalArgumentException("" + value);
        }
    }

    public static @Nullable Expression getDefaultValue(final Type type,
                                                       final boolean nullable) {
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
        } else if (nullable) {
            return new NullLiteralExpr();
        } else if (isListType(type)) {
            return new MethodCallExpr()
                    .setScope(new NameExpr("java.util.List"))
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
            return declaredType.getNameWithScope().equals("java.util.List");
        }
    }

    public static boolean isClassOrInterfaceType(final Type type,
                                                 final String name) {
        if (!type.isClassOrInterfaceType()) {
            return false;
        } else {
            final var declaredType = (ClassOrInterfaceType) type;
            return declaredType.getNameWithScope().equals(name);
        }
    }
}
