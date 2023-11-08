package io.github.potjerodekool.openapi.internal.generate.annotation;

import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.expression.ArrayInitializerExpression;
import io.github.potjerodekool.codegen.model.tree.expression.Expression;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GenericAnnotationBuilder {

    private final String annotationClassName;

    private final Map<String, Expression> members = new HashMap<>();

    public GenericAnnotationBuilder(final String annotationClassName) {
        this.annotationClassName = annotationClassName;
    }

    public GenericAnnotationBuilder add(final String name,
                                        final Expression expression) {
        this.members.put(name, expression);
        return this;
    }

    public GenericAnnotationBuilder add(final String name,
                                        final Expression... expressions) {
        return add(name, new ArrayInitializerExpression(List.of(expressions)));
    }

    public GenericAnnotationBuilder add(final String name,
                                        final List<Expression> expressions) {
        return add(name, new ArrayInitializerExpression(expressions));
    }

    public AnnotationExpression build() {
        return new AnnotationExpression(
                new ClassOrInterfaceTypeExpression(annotationClassName),
                new HashMap<>(members)
        );
    }
}
