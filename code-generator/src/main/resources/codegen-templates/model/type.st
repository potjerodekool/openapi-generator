type(type)::= <%
<if(type.typeKind.isWILDCARD)>
    <if(type.extendsBound)>
    extends <type(type.extendsBound)>
    <endif>
<elseif(type.typeKind.isTYPEVAR)>
 <type.name> <type(type.bounds)>
<else>
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
<if(type.typeArguments)>
<typeArguments(type.typeArguments)>
<endif>
<endif>
%>
