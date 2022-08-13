package io.github.potjerodekool.openapi.internal.generate;

import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.ArrayInitializerExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.type.Type;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.List;

public interface GenerateUtils {
    boolean isListType(Type type);

    @Nullable Expression getDefaultValue(Type type);


    AnnotationExpr createAnnotation(String name,
                                    String memberName,
                                    AnnotationExpr value);

    NormalAnnotationExpr createAnnotation(String name,
                                          AnnotationMember member);

    NormalAnnotationExpr createAnnotation(String name,
                                          List<AnnotationMember> members);

    <A extends AnnotationExpr> ArrayInitializerExpr createArrayInitializerExprOfAnnotations(List<@NonNull A> list);

    ArrayInitializerExpr createArrayInitializerExprOfStrings(List<@NonNull String> list);

    <E extends Expression> ArrayInitializerExpr createArrayInitializerExpr(List<@NonNull E> list);

    <E extends Expression> Expression toExpression(List<@NonNull E> list);

    Type getFirstTypeArg(Type type);

    AnnotationExpr createArraySchemaAnnotation(Type elementType);

    AnnotationExpr createSchemaAnnotation(Type type,
                                          Boolean required);

    AnnotationExpr createSchemaAnnotation(String type,
                                          @Nullable String format);

    AnnotationExpr createEmptySchemaAnnotation();

    Type getFieldType(FieldDeclaration fieldDeclaration);
}
