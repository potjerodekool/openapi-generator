package io.github.potjerodekool.openapi.internal.ast.util;

class TypeUtilsTest {

    /*
    private final JavaTypes types = new JavaTypes(new ReflectionTypeElementLoader(getClass().getClassLoader()));
    private final TypeUtils typeUtils = new TypeUtils(
            types
    );

    private final OpenApiTypeUtils openApiTypeUtils = new OpenApiTypeUtils(typeUtils, types);

    @Test
    void createStringType() {
        assertEquals("java.lang.String", types.getStringType().asElement().getQualifiedName().toString());
    }

    @Test
    void createVoidType() {
        assertEquals("java.lang.Void", types.getVoidType().asElement().getQualifiedName().toString());
    }

    @Test
    void createDeclaredType() {
        assertEquals("java.lang.Integer", types.getDeclaredType("java.lang.Integer").asElement().getQualifiedName().toString());
    }
    @Test
    void createType() {
        assertDeclaredType("java.lang.String", openApiTypeUtils.createType(
                new OpenApiStandardType(OpenApiStandardTypeEnum.STRING, null,false )));
        assertDeclaredType("java.time.LocalDate", openApiTypeUtils.createType(
                new OpenApiStandardType(OpenApiStandardTypeEnum.STRING, "date",false )));
        assertDeclaredType("java.time.LocalDateTime", openApiTypeUtils.createType(
                new OpenApiStandardType(OpenApiStandardTypeEnum.STRING, "date-time",false )));
        assertDeclaredType("java.time.LocalTime", openApiTypeUtils.createType(
                new OpenApiStandardType(OpenApiStandardTypeEnum.STRING, "time",false )));
        assertDeclaredType("java.util.UUID", openApiTypeUtils.createType(
                new OpenApiStandardType(OpenApiStandardTypeEnum.STRING, "uuid",false )));

        final var resourceType = openApiTypeUtils.createType(
                new OpenApiStandardType(OpenApiStandardTypeEnum.STRING, "binary",false ));
        assertTrue(resourceType.isWildCardType());

        final var wildCardType = (WildcardType) resourceType;
        assertDeclaredType("org.springframework.core.io.Resource", (Type) wildCardType.getExtendsBound());
    }

    private void assertDeclaredType(final String className,
                                    final Type schema) {
        assertTrue(schema.isDeclaredType());
        final var declaredType = (DeclaredType) schema;
        assertEquals(className, declaredType.asElement().getQualifiedName().toString());
    }

    private void assertDeclaredType(final String className,
                                    final List<String> typeArguments,
                                    final Type schema) {
        assertTrue(schema.isDeclaredType());
        final var declaredType = (DeclaredType) schema;
        assertEquals(className, declaredType.asElement().getQualifiedName().toString());

        final var typeArgs = declaredType.getTypeArguments();

        for (int i = 0; i < typeArguments.size(); i++) {
            final var typeArgument = typeArguments.get(i);
            assertDeclaredType(
                    typeArgument,
                    (Type) typeArgs.get(i)
            );
        }
    }

    @Test
    void isMapType() {
        //assertTrue(typeUtils.isMapType(typeUtils.createMapType()));
    }

    @Test
    void getBoxedType() {
        assertEquals("java.lang.Boolean", types.boxedClass(types.getPrimitiveType(TypeKind.BOOLEAN))
                .getQualifiedName().toString());
        assertEquals("java.lang.Byte", types.boxedClass(types.getPrimitiveType(TypeKind.BYTE))
                .getQualifiedName().toString());
        assertEquals("java.lang.Short", types.boxedClass(types.getPrimitiveType(TypeKind.SHORT))
                .getQualifiedName().toString());
        assertEquals("java.lang.Integer", types.boxedClass(types.getPrimitiveType(TypeKind.INT))
                .getQualifiedName().toString());
        assertEquals("java.lang.Long", types.boxedClass(types.getPrimitiveType(TypeKind.LONG))
                .getQualifiedName().toString());
        assertEquals("java.lang.Character", types.boxedClass(types.getPrimitiveType(TypeKind.CHAR))
                .getQualifiedName().toString());
        assertEquals("java.lang.Float", types.boxedClass(types.getPrimitiveType(TypeKind.FLOAT))
                .getQualifiedName().toString());
        assertEquals("java.lang.Double", types.boxedClass(types.getPrimitiveType(TypeKind.DOUBLE))
                .getQualifiedName().toString());
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
                types.getStringType(),
                false
        ));
    }

    @Test
    void getListTypeName() {
        assertEquals("java.util.List", typeUtils.getListTypeName());
    }

    @Test
    void isStringType() {
        assertTrue(typeUtils.isStringType(types.getStringType()));
        assertFalse(typeUtils.isStringType(types.getDeclaredType("java.lang.Integer")));
    }

    @Test
    void createCharSequenceType() {
        assertDeclaredType("java.lang.CharSequence", typeUtils.createCharSequenceType());
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
        final var arrayType = types.getArrayType(types.getStringType());
        assertTrue(arrayType.isArrayType());
        assertDeclaredType("java.lang.String", arrayType.getComponentType());
    }

    @Test
    void createKotlinArray() {
        final var arrayType = typeUtils.createKotlinArray(types.getDeclaredType("kotlin.String"));
        assertTrue(arrayType.isArrayType());
        assertDeclaredType("kotlin.String", arrayType.getComponentType());
    }

    @Test
    void createErrorType() {
        assertEquals(TypeKind.ERROR, types.getErrorType().getKind());
    }
     */
}