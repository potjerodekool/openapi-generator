package io.github.potjerodekool.openapi.internal.ast.util;

import io.github.potjerodekool.openapi.internal.ast.type.DeclaredType;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import io.github.potjerodekool.openapi.internal.ast.type.java.WildcardType;
import io.github.potjerodekool.openapi.type.OpenApiStandardType;
import io.github.potjerodekool.openapi.type.OpenApiStandardTypeEnum;
import org.junit.jupiter.api.Test;

import javax.lang.model.type.TypeKind;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TypeUtilsTest {

    private final TypeUtils typeUtils = new TypeUtils(
            TypeUtilsTest.class.getClassLoader()
    );

    @Test
    void createStringType() {
        assertEquals("java.lang.String", typeUtils.createStringType().getElement().getQualifiedName());
    }

    @Test
    void createVoidType() {
        assertEquals("java.lang.Void", typeUtils.createVoidType().getElement().getQualifiedName());
    }

    @Test
    void createDeclaredType() {
        assertEquals("java.lang.Integer", typeUtils.createDeclaredType("java.lang.Integer").getElement().getQualifiedName());
    }
    @Test
    void createType() {
        assertDeclaredType("java.lang.String", typeUtils.createType(
                new OpenApiStandardType(OpenApiStandardTypeEnum.STRING, null,false )));
        assertDeclaredType("java.time.LocalDate", typeUtils.createType(
                new OpenApiStandardType(OpenApiStandardTypeEnum.STRING, "date",false )));
        assertDeclaredType("java.time.LocalDateTime", typeUtils.createType(
                new OpenApiStandardType(OpenApiStandardTypeEnum.STRING, "date-time",false )));
        assertDeclaredType("java.time.LocalTime", typeUtils.createType(
                new OpenApiStandardType(OpenApiStandardTypeEnum.STRING, "time",false )));
        assertDeclaredType("java.util.UUID", typeUtils.createType(
                new OpenApiStandardType(OpenApiStandardTypeEnum.STRING, "uuid",false )));

        final var resourceType = typeUtils.createType(
                new OpenApiStandardType(OpenApiStandardTypeEnum.STRING, "binary",false ));
        assertTrue(resourceType.isWildCardType());

        final var wildCardType = (WildcardType) resourceType;
        assertDeclaredType("org.springframework.core.io.Resource", wildCardType.getExtendsBound().get());
    }

    private void assertDeclaredType(final String className,
                                    final Type<?> type) {
        assertTrue(type.isDeclaredType());
        final var declaredType = (DeclaredType) type;
        assertEquals(className, declaredType.getElement().getQualifiedName());
    }

    private void assertDeclaredType(final String className,
                                    final List<String> typeArguments,
                                    final Type<?> type) {
        assertTrue(type.isDeclaredType());
        final var declaredType = (DeclaredType) type;
        assertEquals(className, declaredType.getElement().getQualifiedName());

        final var typeArgs = declaredType.getTypeArguments().get();

        for (int i = 0; i < typeArguments.size(); i++) {
            final var typeArgument = typeArguments.get(i);
            assertDeclaredType(
                    typeArgument,
                    typeArgs.get(i)
            );
        }
    }

    @Test
    void isMapType() {
        assertTrue(typeUtils.isMapType(typeUtils.createMapType()));
    }

    @Test
    void getBoxedType() {
        assertEquals("java.lang.Boolean", typeUtils.getBoxedType(typeUtils.createPrimitiveType(TypeKind.BOOLEAN))
                .getElement().getQualifiedName());
        assertEquals("java.lang.Byte", typeUtils.getBoxedType(typeUtils.createPrimitiveType(TypeKind.BYTE))
                .getElement().getQualifiedName());
        assertEquals("java.lang.Short", typeUtils.getBoxedType(typeUtils.createPrimitiveType(TypeKind.SHORT))
                .getElement().getQualifiedName());
        assertEquals("java.lang.Integer", typeUtils.getBoxedType(typeUtils.createPrimitiveType(TypeKind.INT))
                .getElement().getQualifiedName());
        assertEquals("java.lang.Long", typeUtils.getBoxedType(typeUtils.createPrimitiveType(TypeKind.LONG))
                .getElement().getQualifiedName());
        assertEquals("java.lang.Character", typeUtils.getBoxedType(typeUtils.createPrimitiveType(TypeKind.CHAR))
                .getElement().getQualifiedName());
        assertEquals("java.lang.Float", typeUtils.getBoxedType(typeUtils.createPrimitiveType(TypeKind.FLOAT))
                .getElement().getQualifiedName());
        assertEquals("java.lang.Double", typeUtils.getBoxedType(typeUtils.createPrimitiveType(TypeKind.DOUBLE))
                .getElement().getQualifiedName());
    }

    @Test
    void isListType() {
        assertTrue(typeUtils.isListType(typeUtils.createListType()));
    }

    @Test
    void createListType() {
        assertDeclaredType("java.util.List", typeUtils.createListType());
    }

    @Test
    void testCreateListType() {
        assertDeclaredType("java.util.List",
                List.of("java.lang.String"),
                typeUtils.createListType(
                typeUtils.createStringType(),
                false
        ));
    }

    @Test
    void getListTypeName() {
        assertEquals("java.util.List", typeUtils.getListTypeName());
    }

    @Test
    void isStringType() {
        assertTrue(typeUtils.isStringType(typeUtils.createStringType()));
        assertFalse(typeUtils.isStringType(typeUtils.createDeclaredType("java.lang.Integer")));
    }

    @Test
    void createCharSequenceType() {
        assertDeclaredType("java.lang.CharSequence", typeUtils.createCharSequenceType());
    }

    @Test
    void isBooleanType() {
        assertTrue(typeUtils.isBooleanType(typeUtils.createDeclaredType("java.lang.Boolean")));
        assertTrue(typeUtils.isBooleanType(typeUtils.createPrimitiveType(TypeKind.BOOLEAN)));
    }

    @Test
    void createMultipartType() {
        assertDeclaredType("org.springframework.web.multipart.MultipartFile", typeUtils.createMultipartType());
    }

    @Test
    void createObjectType() {
        assertDeclaredType("java.lang.Object", typeUtils.createObjectType());
    }

    @Test
    void createArray() {
        final var arrayType = typeUtils.createArray(typeUtils.createStringType());
        assertTrue(arrayType.isArrayType());
        assertDeclaredType("java.lang.String", arrayType.getComponentType());
    }

    @Test
    void createKotlinArray() {
        final var arrayType = typeUtils.createKotlinArray(typeUtils.createDeclaredType("kotlin.String"));
        assertTrue(arrayType.isArrayType());
        assertDeclaredType("kotlin.String", arrayType.getComponentType());
    }

    @Test
    void createErrorType() {
        assertEquals(TypeKind.ERROR, typeUtils.createErrorType().getKind());
    }
}