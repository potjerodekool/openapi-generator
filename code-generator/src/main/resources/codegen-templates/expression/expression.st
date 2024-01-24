expression(expression)::= <%
    <if(expression.kind.isLITERAL)>
        </expression/literalExpression(expression)>
    <elseif(expression.kind.isFIELD_ACCESS)>
        </expression/fieldAccessExpression(expression)>
    <elseif(expression.kind.isCLASS_TYPE)>
        <expression.name>
    <elseif(expression.kind.isIDENTIFIER)>
        <expression.name>
    <else>
        unknown expression: <expression.kind>
    <endif>
%>