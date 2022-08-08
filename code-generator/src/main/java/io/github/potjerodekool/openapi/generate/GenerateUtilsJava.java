package io.github.potjerodekool.openapi.generate;

import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import org.checkerframework.checker.nullness.qual.Nullable;

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
}
