package io.github.potjerodekool.openapi.internal.ast.expression.kotlin;

import io.github.potjerodekool.openapi.internal.ast.expression.AnnotationExpression;
import io.github.potjerodekool.openapi.internal.ast.expression.Expression;

import java.util.Map;

public class KotlinAnnotationExpression extends AnnotationExpression {

    private final String prefix;

    public KotlinAnnotationExpression(final String prefix,
                                      final String annotationClassName,
                                      final Map<String, Expression> elementValues) {
        super(annotationClassName, elementValues);
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
