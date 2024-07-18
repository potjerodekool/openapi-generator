package io.github.potjerodekool.openapi.spring.web.generate.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.template.model.element.MethodElem;
import io.github.potjerodekool.codegen.template.model.element.TypeElem;
import io.github.potjerodekool.codegen.template.model.expression.*;
import io.github.potjerodekool.codegen.template.model.statement.BlockStm;
import io.github.potjerodekool.codegen.template.model.statement.ReturnStm;
import io.github.potjerodekool.codegen.template.model.type.ClassOrInterfaceTypeExpr;
import io.github.potjerodekool.openapi.common.ApiConfiguration;
import io.github.potjerodekool.openapi.common.GeneratorConfig;
import io.github.potjerodekool.openapi.common.generate.OpenApiTypeUtils;
import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;

public class SpringApiGenerator extends AbstractApiGenerator {

    public SpringApiGenerator(final GeneratorConfig generatorConfig,
                              final ApiConfiguration apiConfiguration,
                              final OpenApiTypeUtils typeUtils,
                              final Environment environment) {
        super(generatorConfig, apiConfiguration, typeUtils, environment);
    }

    @Override
    protected TypeElem createClass(final String simpleName) {
        return super.createClass(simpleName)
                .kind(ElementKind.INTERFACE);
    }

    @Override
    protected String classNameSuffix() {
        return "Api";
    }

    @Override
    protected void postProcessOperation(final OpenAPI openAPI,
                                        final HttpMethod httpMethod,
                                        final String path,
                                        final Operation operation,
                                        final MethodElem method) {
        method.modifier(Modifier.DEFAULT);

        addMethodAnnotations(
                openAPI,
                httpMethod,
                path,
                operation,
                method
        );

        final var body = new BlockStm();

        body.statement(new ReturnStm(
                new MethodInvocationExpr()
                        .target(new ClassOrInterfaceTypeExpr("org.springframework.http.ResponseEntity"))
                        .name("status")
                        .argument(new FieldAccessExpr()
                                .target(new IdentifierExpr("org.springframework.http.HttpStatus"))
                                .field(new IdentifierExpr("NOT_IMPLEMENTED"))
                        )
                        .invoke("build")
        ));

        method.body(body);
    }

}
