package io.github.potjerodekool.openapi.internal.ast.element;

import io.github.potjerodekool.openapi.internal.ast.Attribute;
import io.github.potjerodekool.openapi.internal.ast.type.Type;

public interface AnnotationValueVisitor<R,P> {

    R visitAnnotation(AnnotationMirror annotation, P param);

    R visitBoolean(boolean value, P param);

    R visitInt(int value, P param);

    R visitLong(long value, P param);

    R visitString(String value, P param);

    R visitArray(Attribute[] array, P param);

    R visitType(Type<?> type, P param);

    R visitEnum(VariableElement enumValue, P param);

}
