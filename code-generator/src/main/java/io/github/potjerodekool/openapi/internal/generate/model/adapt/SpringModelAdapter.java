package io.github.potjerodekool.openapi.internal.generate.model.adapt;

import io.github.potjerodekool.codegen.model.CompilationUnit;
import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.JTreeFilter;
import io.github.potjerodekool.codegen.model.tree.expression.FieldAccessExpression;
import io.github.potjerodekool.codegen.model.tree.statement.ClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.media.DateSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;

public class SpringModelAdapter implements ModelAdapter {
    @Override
    public void adapt(final HttpMethod method, final ObjectSchema schema, final CompilationUnit unit) {
        final var classDeclarations = unit.getClassDeclarations();

        if (classDeclarations.isEmpty()) {
            return;
        }

        final var classDeclaration = classDeclarations.get(0);

        schema.getProperties().forEach((propertyName, propertySchema) -> {
            if (propertySchema instanceof DateSchema) {
                adaptDateProperty(propertyName, classDeclaration);
            }
        });
    }

    private void adaptDateProperty(final String propertyName,
                                   final ClassDeclaration classDeclaration) {
        final var fieldOptional = JTreeFilter.fields(classDeclaration).stream()
                .filter(field -> field.getName().equals(propertyName))
                .findFirst();

        fieldOptional.ifPresent(field -> field.annotation(new AnnotationExpression()
                .annotationType(new ClassOrInterfaceTypeExpression("org.springframework.format.annotation.DateTimeFormat"))
                .argument("iso", new FieldAccessExpression()
                        .scope(new ClassOrInterfaceTypeExpression("org.springframework.format.annotation.DateTimeFormat"))
                        .field(Name.of("ISO")))
        ));
    }
}
