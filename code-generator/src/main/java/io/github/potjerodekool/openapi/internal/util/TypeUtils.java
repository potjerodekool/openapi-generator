package io.github.potjerodekool.openapi.internal.util;

import io.github.potjerodekool.codegen.model.symbol.ClassSymbol;
import io.github.potjerodekool.codegen.model.tree.expression.Expression;
import io.github.potjerodekool.codegen.model.tree.expression.NameExpression;
import io.github.potjerodekool.codegen.model.tree.type.ParameterizedType;
import io.github.potjerodekool.codegen.model.tree.type.TypeExpression;
import io.github.potjerodekool.codegen.model.type.AbstractType;
import io.github.potjerodekool.codegen.model.type.DeclaredType;
import io.github.potjerodekool.codegen.model.type.TypeMirror;
import io.github.potjerodekool.codegen.model.util.Elements;
import io.github.potjerodekool.codegen.model.util.type.Types;

public final class TypeUtils {

    private static final String OBJECT_CLASS_NAME = "java.lang.Object";
    private static final String STRING_CLASS_NAME = "java.lang.String";
    private static final String LIST_CLASS_NAME = "java.util.List";
    private static final String MAP_CLASS_NAME = "java.util.Map";
    private static final String CHAR_SEQUENCE_NAME = "java.lang.CharSequence";
    private static final String VOID_NAME = "java.lang.Void";

    private final Types types;
    private final Elements elements;

    public TypeUtils(final Types types,
                     final Elements elements) {
        this.types = types;
        this.elements = elements;
    }

    public DeclaredType getDeclaredType(final String className,
                                        final boolean nullable) {
        final var declaredType = (AbstractType) types.getDeclaredType(elements.getTypeElement(className));

        if (nullable) {
            return (DeclaredType) declaredType.asNullableType();
        } else {
            return (DeclaredType) declaredType;
        }
    }

    public boolean isStringType(final TypeMirror type) {
        final var stringType = (TypeMirror) elements.getTypeElement(STRING_CLASS_NAME).asType();
        return type.accept(new BasicAssignableTypeVisitor(), stringType);
    }

    public boolean isListType(final TypeMirror type) {
        final var listType = (TypeMirror) elements.getTypeElement((LIST_CLASS_NAME)).asType();
        return type.accept(new BasicAssignableTypeVisitor(), listType);
    }

    public boolean isMapType(final TypeMirror type) {
        final var mapType = (TypeMirror) elements.getTypeElement((MAP_CLASS_NAME)).asType();
        return type.accept(new BasicAssignableTypeVisitor(), mapType);
    }

    public DeclaredType createListType() {
        return types.getDeclaredType(elements.getTypeElement(LIST_CLASS_NAME), createObjectType(false));
    }

    public DeclaredType createListType(final DeclaredType dt, final boolean nullable) {
        var listType = createListType();
        listType = types.getDeclaredType((ClassSymbol) listType.asElement(), dt);

        if (nullable) {
            listType = listType.asNullableType();
        }

        return listType;
    }

    public String getListTypeName() {
        return LIST_CLASS_NAME;
    }

    public DeclaredType createMapType() {
        return types.getDeclaredType(elements.getTypeElement(MAP_CLASS_NAME), createObjectType(false), createObjectType(false));
    }

    public DeclaredType createMapType(final DeclaredType keyType,
                                      final DeclaredType valueType,
                                      final boolean nullable) {
        var mapType = createMapType();
        mapType = types.getDeclaredType((ClassSymbol) mapType.asElement(), keyType, valueType);
        if (nullable) {
            mapType = mapType.asNullableType();
        }
        return mapType;
    }

    public DeclaredType createObjectType() {
        return types.getDeclaredType(elements.getTypeElement(OBJECT_CLASS_NAME));
    }

    public DeclaredType createObjectType(final boolean nullable) {
        var objectType = createObjectType();

        if (nullable) {
            objectType = objectType.asNullableType();
        }

        return objectType;
    }

    public DeclaredType getStringType() {
        return types.getDeclaredType(elements.getTypeElement(STRING_CLASS_NAME));
    }

    public DeclaredType getStringType(final boolean isNullable) {
        final var stringType = getStringType();

        if (isNullable) {
            return stringType.asNullableType();
        } else {
            return stringType;
        }
    }

    public DeclaredType getVoidType() {
        return types.getDeclaredType(elements.getTypeElement(VOID_NAME));
    }

    public DeclaredType getCharSequenceType() {
        return getDeclaredType(CHAR_SEQUENCE_NAME, false);
    }

    public DeclaredType createMultipartType() {
        return types.getDeclaredType(elements.getTypeElement("org.springframework.web.multipart.MultipartFile"));
    }

    public Expression createMultipartTypeExpression() {
        return new NameExpression("org.springframework.web.multipart.MultipartFile");
    }
}
