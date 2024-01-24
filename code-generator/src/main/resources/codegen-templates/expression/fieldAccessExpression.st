fieldAccessExpression(fieldAccess)::= <%
    <if(fieldAccess.target)>
        <expression(fieldAccess.target)>.
    <endif>
    <expression(fieldAccess.field)>
%>