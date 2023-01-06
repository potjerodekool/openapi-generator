package io.github.potjerodekool.openapi.internal.ast;

import io.github.potjerodekool.openapi.internal.ast.element.*;
import io.github.potjerodekool.openapi.internal.ast.type.DeclaredType;
import io.github.potjerodekool.openapi.internal.ast.type.Type;
import io.github.potjerodekool.openapi.internal.ast.type.TypeFactory;
import io.github.potjerodekool.openapi.internal.util.QualifiedName;

import javax.lang.model.element.ElementKind;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class Attribute implements AnnotationValue {

    public static Attribute constant(final boolean value) {
        return new Constant(value);
    }

    public static Attribute constant(final long value) {
        return new Constant(value);
    }

    public static Attribute constant(final int value) {
        return new Constant(value);
    }

    public static Attribute constant(final String value) {
        return new Constant(value);
    }

    public static Attribute array(final Attribute value) {
        return array(new Attribute[]{value});
    }

    public static Attribute.Array array(final Attribute[] values) {
        return new Array(values);
    }

    public static Attribute.Array array(final List<? extends Attribute> values) {
        return new Array(values.toArray(Attribute[]::new));
    }

    public static Attribute clazz(final Type<?> type) {
        return new Class(type);
    }

    public static Compound compound(final String className) {
        return new Compound(TypeFactory.createDeclaredType(ElementKind.ANNOTATION_TYPE, className));
    }

    public static Compound compound(final String className,
                                    final Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues) {
        return new Compound(TypeFactory.createDeclaredType(ElementKind.ANNOTATION_TYPE, className), elementValues);
    }

    public static Compound compound(final String className,
                                    final AnnotationValue elementValue) {
        return new Compound(TypeFactory.createDeclaredType(ElementKind.ANNOTATION_TYPE, className), Map.of(
                MethodElement.createMethod("value"), elementValue
        ));
    }

    public static Enum createEnumAttribute(final VariableElement variableElement) {
        return new Enum(variableElement);
    }

    public static Enum createEnumAttribute(final String className,
                                           final String variableName) {
        final var qualifiedName = QualifiedName.from(className);
        final var packageElement = PackageElement.create(qualifiedName.packageName());
        final var typeElement = TypeElement.createClass(qualifiedName.simpleName());
        typeElement.setEnclosingElement(packageElement);
        final var variableElement = VariableElement.createField(variableName, typeElement.asType());
        variableElement.setEnclosingElement(typeElement);
        return createEnumAttribute(variableElement);
    }

    public static class Constant extends Attribute {

        private final Object value;

        private Constant(final Object value) {
            this.value = value;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public <R, P> R accept(final AnnotationValueVisitor<R, P> visitor, final P param) {
            if (value instanceof Boolean b) {
                return visitor.visitBoolean(b, param);
            } else if (value instanceof Integer i) {
                return visitor.visitInt(i, param);
            } else if (value instanceof Long l) {
                return visitor.visitLong(l, param);
            } else if (value instanceof String s) {
                return visitor.visitString(s, param);
            } else {
                throw new UnsupportedOperationException("" + value);
            }
        }

    }

    public static class Array extends Attribute {

        private final Attribute[] values;

        private Array(final Attribute[] values) {
            this.values = values;
        }

        @Override
        public Attribute[] getValue() {
            return values;
        }

        @Override
        public <R, P> R accept(final AnnotationValueVisitor<R, P> visitor, final P param) {
            return visitor.visitArray(values, param);
        }
    }

    public static class Class extends Attribute {

        private final Type<?> classType;

        private Class(final Type<?> classType) {
            this.classType = classType;
        }

        @Override
        public Type<?> getValue() {
            return classType;
        }

        @Override
        public <R, P> R accept(final AnnotationValueVisitor<R, P> visitor, final P param) {
            return visitor.visitType(classType, param);
        }
    }

    public static class Compound extends Attribute implements AnnotationMirror {

        private final DeclaredType annotationType;
        private final Map<ExecutableElement, AnnotationValue> elementValues = new HashMap<>();

        private Compound(final DeclaredType annotationType) {
            this(annotationType, Map.of());
        }

        private Compound(final DeclaredType annotationType,
                         final Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues) {
            this.annotationType = annotationType;
            this.elementValues.putAll(elementValues);
        }

        @Override
        public DeclaredType getAnnotationType() {
            return annotationType;
        }

        @Override
        public Map<? extends ExecutableElement, ? extends AnnotationValue> getElementValues() {
            return elementValues;
        }

        @Override
        public Object getValue() {
            return this;
        }

        @Override
        public <R, P> R accept(final AnnotationValueVisitor<R, P> visitor, final P param) {
            return visitor.visitAnnotation(this, param);
        }

        public void addElementValue(final String name, final AnnotationValue value) {
            this.elementValues.put(MethodElement.createMethod(name), value);
        }
    }

    public static class Enum extends Attribute {

        private final VariableElement variableElement;

        public Enum(final VariableElement variableElement) {
            this.variableElement = variableElement;
        }

        @Override
        public VariableElement getValue() {
            return variableElement;
        }

        @Override
        public <R, P> R accept(final AnnotationValueVisitor<R, P> visitor, final P param) {
            return visitor.visitEnum(variableElement, param);
        }
    }



}
