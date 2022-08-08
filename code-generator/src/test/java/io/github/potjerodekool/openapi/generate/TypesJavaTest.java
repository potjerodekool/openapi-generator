package io.github.potjerodekool.openapi.generate;

import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;
import io.github.potjerodekool.openapi.tree.Package;
import io.github.potjerodekool.openapi.type.OpenApiArrayType;
import io.github.potjerodekool.openapi.type.OpenApiObjectType;
import io.github.potjerodekool.openapi.type.OpenApiStandardType;
import io.github.potjerodekool.openapi.type.OpenApiStandardTypeEnum;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TypesJavaTest {

    private final TypesJava types = new TypesJava();

    @Test
    void createTypeStandard() {
        final var stringType = (ClassOrInterfaceType) types.createType(new OpenApiStandardType(OpenApiStandardTypeEnum.STRING, null, true));
        assertEquals("java.lang.String", stringType.getNameWithScope());

        final var primitiveTypeInteger = (PrimitiveType) types.createType(new OpenApiStandardType(OpenApiStandardTypeEnum.INTEGER, "int32", false));
        assertEquals(PrimitiveType.Primitive.INT, primitiveTypeInteger.getType());

        final var longType = (ClassOrInterfaceType) types.createType(new OpenApiStandardType(OpenApiStandardTypeEnum.INTEGER, "int32", true));
        assertEquals("java.lang.Integer", longType.getNameWithScope());

        final var primitiveTypeLong = (PrimitiveType) types.createType(new OpenApiStandardType(OpenApiStandardTypeEnum.INTEGER, "int64", false));
        assertEquals(PrimitiveType.Primitive.LONG, primitiveTypeLong.getType());

        final var integerType = (ClassOrInterfaceType) types.createType(new OpenApiStandardType(OpenApiStandardTypeEnum.INTEGER, "int64", true));
        assertEquals("java.lang.Long", integerType.getNameWithScope());

        final var dateType = (ClassOrInterfaceType) types.createType(new OpenApiStandardType(OpenApiStandardTypeEnum.STRING, "date", false));
        assertEquals("java.time.LocalDate", dateType.getNameWithScope());

        final var dateTimeType = (ClassOrInterfaceType) types.createType(new OpenApiStandardType(OpenApiStandardTypeEnum.STRING, "date-time", false));
        assertEquals("java.time.LocalDateTime", dateTimeType.getNameWithScope());
    }

    @Test
    void createTypeArray() {
        final var itemType = new OpenApiStandardType(OpenApiStandardTypeEnum.STRING, null, true);
        final var arrayType = (ClassOrInterfaceType) types.createType(new OpenApiArrayType(itemType));
        assertEquals("java.util.List", arrayType.getNameWithScope());

        final var typeArgumentsOptional = arrayType.getTypeArguments();
        assertTrue(typeArgumentsOptional.isPresent());
        final var typeArgs = typeArgumentsOptional.get();
        assertEquals(1, typeArgs.size());
        final var listItemType = (ClassOrInterfaceType) typeArgs.get(0);
        assertEquals("java.lang.String", listItemType.getNameWithScope());
    }

    @Test
    void createTypeObjectType() {
        final var stringType = new OpenApiStandardType(OpenApiStandardTypeEnum.STRING, null, true);
        final var objectType = new OpenApiObjectType("User", Map.of(
                "firstName",
                new OpenApiProperty(
                        stringType,
                        true,
                        false,
                        null
                )
            ),
                null).withPackage(new Package("org.some.api.model"));

        final var type = (ClassOrInterfaceType) types.createType(objectType);
        assertEquals("org.some.api.model.User", type.getNameWithScope());
    }

    @Test
    void createTypeObjectTypeNoPackage() {
        final var stringType = new OpenApiStandardType(OpenApiStandardTypeEnum.STRING, null, true);
        final var objectType = new OpenApiObjectType("User", Map.of(
                "firstName",
                new OpenApiProperty(
                        stringType,
                        true,
                        false,
                        null
                )
        ),
                null);

        final var type = (ClassOrInterfaceType) types.createType(objectType);
        assertEquals("User", type.getNameWithScope());
    }

    @Test
    void testCreateType() {
        final var classOrInterfaceType = types.createType("java.util.List");
        assertEquals("java.util.List", classOrInterfaceType.getNameWithScope());
    }
}