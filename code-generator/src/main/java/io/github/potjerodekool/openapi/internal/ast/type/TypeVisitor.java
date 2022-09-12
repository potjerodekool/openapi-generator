package io.github.potjerodekool.openapi.internal.ast.type;

import io.github.potjerodekool.openapi.internal.ast.type.kotlin.KotlinArray;

public interface TypeVisitor<R, P> {

    R visitUnknownType(Type<?> type, P param);

    R visitVoidType(VoidType voidType,
                    P param);

    R visitPackageType(PackageType packageType,
                       P param);

    R visitDeclaredType(DeclaredType declaredType,
                        P param);

    R visitExecutableType(ExecutableType executableType,
                          P param);

    R visitJavaArrayType(JavaArrayType javaArrayType,
                         P param);

    R visitBooleanType(PrimitiveType booleanType,
                       P param);

    R visitByteType(PrimitiveType byteType,
                    P param);

    R visitShortType(PrimitiveType shortType,
                     P param);

    R visitIntType(PrimitiveType intType,
                   P param);

    R visitLongType(PrimitiveType longType,
                    P param);

    R visitCharType(PrimitiveType charType,
                    P param);

    R visitFloatType(PrimitiveType floatType,
                     P param);

    R visitDoubleType(PrimitiveType doubleType,
                      P param);

    R visitWildcardType(WildcardType wildcardType,
                        P param);

    //Kotlin types
    default R visitUnitType(final UnitType unitType,
                            final P param) {
        return visitUnknownType(unitType, param);
    }

    default R visitKotlinArray(final KotlinArray kotlinArray,
                               final P param) {
        return visitUnknownType(kotlinArray, param);
    }
}
