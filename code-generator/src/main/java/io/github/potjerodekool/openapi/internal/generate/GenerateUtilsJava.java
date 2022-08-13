package io.github.potjerodekool.openapi.internal.generate;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.WildcardType;
import io.github.potjerodekool.openapi.internal.util.GenerateException;
import io.github.potjerodekool.openapi.internal.util.NodeListCollectors;
import io.github.potjerodekool.openapi.internal.util.Utils;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public final class GenerateUtilsJava implements GenerateUtils {

    private final Types types;

    public GenerateUtilsJava(final Types types) {
        this.types = types;
    }

    @Override
    public boolean isListType(final Type type) {
        if (!type.isClassOrInterfaceType()) {
            return false;
        } else {
            final var declaredType = (ClassOrInterfaceType) type;
            return this.types.isListTypeName(declaredType.getNameWithScope());
        }
    }

    @Override
    public @Nullable Expression getDefaultValue(final Type type) {
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
                    .setScope(new NameExpr(types.getListTypeName()))
                    .setName("of");
        } else if (type.isArrayType()) {
            return new ArrayInitializerExpr();
        } else {
            return null;
        }
    }

    @Override
    public AnnotationExpr createAnnotation(final String name,
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

    @Override
    public NormalAnnotationExpr createAnnotation(final String name,
                                                 final AnnotationMember member) {
        return createAnnotation(name, List.of(member));
    }

    @Override
    public NormalAnnotationExpr createAnnotation(final String name,
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

    @Override
    public <A extends AnnotationExpr> ArrayInitializerExpr createArrayInitializerExprOfAnnotations(final List<@NonNull A> list) {
        return new ArrayInitializerExpr(list.stream().collect(NodeListCollectors.collector()));
    }

    @Override
    public ArrayInitializerExpr createArrayInitializerExprOfStrings(final List<@NonNull String> list) {
        final NodeList<Expression> nodeList = list.stream()
                .map(this::createLiteralExpression)
                .collect(NodeListCollectors.collector());
        return new ArrayInitializerExpr(nodeList);
    }

    @Override
    public <E extends Expression> ArrayInitializerExpr createArrayInitializerExpr(final List<@NonNull E> list) {
        return new ArrayInitializerExpr(list.stream().collect(NodeListCollectors.collector()));
    }

    @Override
    public <E extends Expression> Expression toExpression(final List<@NonNull E> list) {
        return list.size() == 1
                ? list.get(0)
                : createArrayInitializerExpr(list);
    }

    private LiteralExpr createLiteralExpression(final int value) {
        return new IntegerLiteralExpr(Integer.toString(value));
    }

    private LiteralExpr createLiteralExpression(final boolean value) {
        return new BooleanLiteralExpr(value);
    }

    private LiteralExpr createLiteralExpression(final String value) {
        return new StringLiteralExpr(value);
    }

    @Override
    public Type getFirstTypeArg(final Type type) {
        if (type instanceof ClassOrInterfaceType cType) {
            return cType.getTypeArguments()
                    .filter(it -> it.size() > 0)
                    .map(it -> it.get(0))
                    .orElseThrow(() -> new GenerateException("Expected a type argument"));
        } else {
            return type;
        }
    }

    @Override
    public AnnotationExpr createArraySchemaAnnotation(final Type elementType) {
        return createAnnotation("io.swagger.v3.oas.annotations.media.ArraySchema", "schema",
                createSchemaAnnotation(elementType, false)
        );
    }

    @Override
    public AnnotationExpr createSchemaAnnotation(final Type type,
                                                 final Boolean required) {
        final var members = new NodeList<MemberValuePair>();

        final Type implementationType;

        if (type instanceof WildcardType wt) {
            if (wt.getExtendedType().isPresent()) {
                implementationType = wt.getExtendedType().get();
            } else if (wt.getSuperType().isPresent()) {
                implementationType = wt.getSuperType().get();
            } else {
                //Will result in compilation error in generated code.
                implementationType = wt;
            }
        } else if (types.isMapType(type)) {
            implementationType = types.createMapType();
        } else {
            implementationType = type;
        }

        members.add(new MemberValuePair("implementation", new ClassExpr(implementationType)));

        if (Utils.isTrue(required)) {
            members.add(new MemberValuePair("required", new BooleanLiteralExpr(true)));
        }

        return new NormalAnnotationExpr(
                new Name("io.swagger.v3.oas.annotations.media.Schema"),
                members
        );
    }

    @Override
    public AnnotationExpr createSchemaAnnotation(final String type,
                                                 final @Nullable String format) {
        final var members = new NodeList<MemberValuePair>();
        members.add(new MemberValuePair("type", new StringLiteralExpr(type)));

        if (format != null) {
            members.add(new MemberValuePair("format", new StringLiteralExpr(format)));
        }

        return new NormalAnnotationExpr(
                new Name("io.swagger.v3.oas.annotations.media.Schema"),
                members
        );
    }


    @Override
    public AnnotationExpr createEmptySchemaAnnotation() {
        return new NormalAnnotationExpr(
                new Name("io.swagger.v3.oas.annotations.media.Schema"),
                NodeList.nodeList()
        );
    }

    @Override
    public Type getFieldType(final FieldDeclaration fieldDeclaration) {
        return fieldDeclaration.getVariable(0).getType();
    }
}
