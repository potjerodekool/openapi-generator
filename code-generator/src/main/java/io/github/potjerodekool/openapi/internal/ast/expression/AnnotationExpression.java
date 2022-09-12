package io.github.potjerodekool.openapi.internal.ast.expression;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class AnnotationExpression implements Expression {

    private final String annotationClassName;

    private final Map<String, Expression> elementValues;

    public AnnotationExpression(final String annotationClassName) {
        this(annotationClassName, new HashMap<>());
    }

    public AnnotationExpression(final String annotationClassName,
                                final Expression value) {
        this(annotationClassName, Map.of("value", value));
    }

    public AnnotationExpression(final String annotationClassName,
                                final Map<String, Expression> elementValues) {
        this.annotationClassName = annotationClassName;
        this.elementValues = elementValues;
    }

    public String getAnnotationClassName() {
        return annotationClassName;
    }

    public Map<String, Expression> getElementValues() {
        return Collections.unmodifiableMap(elementValues);
    }

    public void addElementValue(final String name,
                                final Expression value) {
        this.elementValues.put(name, value);
    }

    @Override
    public <R, P> R accept(final ExpressionVisitor<R, P> visitor, final P param) {
        return visitor.visitAnnotationExpression(this, param);
    }
}
