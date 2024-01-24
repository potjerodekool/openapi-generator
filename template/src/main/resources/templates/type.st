type(type)::= <%
<type.name>

<if(type.typeArgs)>
\<<type.typeArgs:{typeArg|<type(typeArg)>}>>
<endif>
%>