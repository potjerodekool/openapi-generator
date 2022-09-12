package io.github.potjerodekool.openapi.internal.generate;

import io.github.potjerodekool.openapi.internal.ast.TypeUtils;
import io.github.potjerodekool.openapi.internal.ast.expression.*;
import io.github.potjerodekool.openapi.internal.ast.expression.Expression;
import io.github.potjerodekool.openapi.internal.ast.type.*;
import io.github.potjerodekool.openapi.internal.util.GenerateException;
import io.github.potjerodekool.openapi.internal.util.Utils;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.HashMap;
import java.util.Map;

public class CodeGenerateUtils {

    private final TypeUtils typeUtils;

    public CodeGenerateUtils(final TypeUtils typeUtils) {
        this.typeUtils = typeUtils;
    }

    public @Nullable Expression getDefaultValue(final Type<?> type) {
        if (type.isPrimitiveType()) {
            final var primitiveType = ((PrimitiveType) type);

            return switch (primitiveType.getKind()) {
                case BOOLEAN -> LiteralExpression.createBooleanLiteralExpression();
                case CHAR -> LiteralExpression.createCharLiteralExpression();
                case BYTE, SHORT, INT -> LiteralExpression.createIntLiteralExpression();
                case LONG -> LiteralExpression.createLongLiteralExpression();
                case FLOAT -> LiteralExpression.createFloatLiteralExpression();
                case DOUBLE -> LiteralExpression.createDoubleLiteralExpression();
                default -> throw new GenerateException(String.format("%s is not a primitive", primitiveType.getKind()));
            };
        } else if (type.getKind() == TypeKind.DECLARED) {
            return LiteralExpression.createNullLiteralExpression();
        } else if (typeUtils.isListType(type)) {
            return new MethodCallExpression(
                    new NameExpression(typeUtils.getListTypeName()),
                    "of"
            );
        } else if (type.isArrayType()) {
            return new ArrayInitializerExpression();
        } else {
            return null;
        }
    }

    public Type<?> getFirstTypeArg(final Type<?> type) {
        final var declaredType = (DeclaredType) type;
        final var typeArgumentsOptional = declaredType.getTypeArguments();

        if (typeArgumentsOptional.isEmpty()) {
            throw new GenerateException("No typeArguments found");
        }

        final var typeArguments = typeArgumentsOptional.get();

        if (typeArguments.isEmpty()) {
            throw new GenerateException("No typeArguments found");
        }

        return typeArguments.get(0);
    }

    public AnnotationExpression createArraySchemaAnnotation(final Type<?> elementType) {
        return createAnnotation("io.swagger.v3.oas.annotations.media.ArraySchema", "schema",
                createSchemaAnnotation(elementType, false)
        );
    }

    public AnnotationExpression createSchemaAnnotation(final Type<?> type,
                                                       final Boolean required) {
        final var members = new HashMap<String, Expression>();

        final Type<?> implementationType;

        if (type instanceof WildcardType wt) {
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

        members.put("implementation",
                LiteralExpression.createClassLiteralExpression(getName(implementationType))
        );

        if (Utils.isTrue(required)) {
            members.put("required", LiteralExpression.createBooleanLiteralExpression(true));
        }

        return new AnnotationExpression(
                "io.swagger.v3.oas.annotations.media.Schema",
                members
        );
    }

    private String getName(final Type<?> type) {
        if (type instanceof DeclaredType dt) {
            return dt.getElement().getQualifiedName();
        } else {
            return "";
        }
    }

    public AnnotationExpression createAnnotation(final String name,
                                                 final String memberName,
                                                 final AnnotationExpression value) {
        return new AnnotationExpression(
                name,
                Map.of(
                        memberName,
                        value
                )
        );
    }
}
