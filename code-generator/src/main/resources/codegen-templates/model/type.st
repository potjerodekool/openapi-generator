type(type)::= <%
<if(type.annotations)>
 <if(type.packageName)>
 <type.packageName>.
 <endif>
<type.annotations:{annotation|<annotation(annotation)>};separator="\n">
<\ >
<type.simpleName>
<else>
<type.name>
<endif>

<if(type.typeArgs)>
\<<type.typeArgs:{typeArg|<type(typeArg)>}>>
<endif>
%>