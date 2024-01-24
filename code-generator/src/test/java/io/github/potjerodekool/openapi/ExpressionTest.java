package io.github.potjerodekool.openapi;

import io.github.potjerodekool.openapi.internal.generate.Templates;
import io.github.potjerodekool.openapi.internal.generate.model.model.expresion.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.openapi.internal.generate.model.model.expresion.FieldAccessExpression;
import io.github.potjerodekool.openapi.internal.generate.model.model.expresion.IdentifierExpression;
import io.github.potjerodekool.openapi.internal.generate.model.model.expresion.LiteralExpression;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ExpressionTest {

    @Test
    void literal() {
        final Templates templates = new Templates();
        final var typeTemplate = templates.getInstanceOf("/expression/expression");
        final var expression = new LiteralExpression("test");
        typeTemplate.add("expression", expression);
        final var code = typeTemplate.render();
        Assertions.assertEquals("\"test\"", code);
    }

    @Test
    void fieldAccess() {
        /*
        <if(fieldAccess.target)>
        <expression(fieldAccess.target)>.
    <endif>
         */

        final Templates templates = new Templates();
        final var typeTemplate = templates.getInstanceOf("/expression/expression");
        final var expression = new FieldAccessExpression()
                .target(new ClassOrInterfaceTypeExpression()
                        .name("org.springframework.format.annotation.DateTimeFormat")
                )
                .field(
                        new FieldAccessExpression()
                                .target(new ClassOrInterfaceTypeExpression()
                                        .name("ISO")
                                )
                                .field(new IdentifierExpression()
                                        .name("DATE")
                                )

                );
        typeTemplate.add("expression", expression);
        final var code = typeTemplate.render();
        Assertions.assertEquals("org.springframework.format.annotation.DateTimeFormat.ISO.DATE", code);
    }
}

/*
expression(expression)::= <%
    <if(expression.kind.isLITERAL)>
        </expression/literalExpression(expression)>
    <elseif(expression.kind.isFIELD_ACCESS)>
        </expression/fieldAccessExpression(expression)>
    <elseif(expression.kind.isCLASS_TYPE)>
        <expression.name>
    <else>
        unknown expression: <expression.kind>
    <endif>
%>
 */