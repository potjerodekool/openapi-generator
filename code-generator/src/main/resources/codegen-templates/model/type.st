type(type)::= <%
<if(type.kind.isWILDCARD)>
    <if(type.extendsBound)>
    extends <type(type.extendsBound)>
    <endif>
<elseif(type.kind.isTYPEVAR)>
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
<if(type.typeArgs)>
<typeArguments(type.typeArgs)>
<endif>
<endif>
%>
