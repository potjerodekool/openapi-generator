package io.github.potjerodekool.openapi.internal.generate.service;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.expression.Expression;
import io.github.potjerodekool.codegen.model.tree.expression.LiteralExpression;
import io.github.potjerodekool.codegen.model.tree.statement.ClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.VariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.NoTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.WildCardTypeExpression;
import io.github.potjerodekool.codegen.model.type.TypeKind;
import io.github.potjerodekool.openapi.ApiConfiguration;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.MissingOperationIdException;
import io.github.potjerodekool.openapi.internal.StatusCodes;
import io.github.potjerodekool.openapi.internal.generate.AbstractGenerator;
import io.github.potjerodekool.openapi.internal.generate.ContentTypes;
import io.github.potjerodekool.openapi.internal.generate.OpenApiUtils;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;
import io.swagger.models.HttpMethod;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ServiceApiGenerator extends AbstractGenerator {

    private final String basePackageName;

    public ServiceApiGenerator(final GeneratorConfig generatorConfig,
                               final ApiConfiguration apiConfiguration,
                               final Environment environment,
                               final OpenApiTypeUtils openApiTypeUtils) {
        super(
                generatorConfig,
                apiConfiguration,
                environment,
                openApiTypeUtils
        );
        this.basePackageName = generatorConfig.basePackageName();
    }

    @Override
    protected List<VariableDeclaration> createParameters(final OpenAPI openAPI,
                                                         final Operation operation,
                                                         final HttpMethod httpMethod) {
        final var parameters = new ArrayList<>(super.createParameters(openAPI, operation, httpMethod));

        final var requestParameter = VariableDeclaration.parameter()
                .modifier(Modifier.FINAL)
                .varType(new ClassOrInterfaceTypeExpression(basePackageName + ".Request"))
                .name("request");
        parameters.add(requestParameter);
        return parameters;
    }

    @Override
    protected void processOperation(final OpenAPI openAPI,
                                    final PathItem openApiPath,
                                    final HttpMethod httpMethod,
                                    final String path,
                                    @Nullable final Operation operation) {
        if (operation == null) {
            return;
        }

        final var operationId = operation.getOperationId();

        if (operationId == null
                || operationId.isEmpty()) {
            throw new MissingOperationIdException(path, httpMethod);
        }

        final var classDeclaration = findOrCreateClassDeclaration(path, openApiPath, operation);

        final var okReponseOptional = OpenApiUtils.findOkResponse(operation.getResponses());

        final Expression responseType;

        if (okReponseOptional.isPresent()) {
            final var okResponse = okReponseOptional.get();

            final var hasContent = okResponse.getValue().getContent() != null
                    && !okResponse.getValue().getContent().isEmpty();

            if (hasContent) {
                final var okResponseType = OpenApiUtils.resolveResponseMediaType(okResponse.getValue().getContent());

                if (okResponseType != null) {
                    final var type = getOpenApiTypeUtils().createTypeExpression(okResponseType, openAPI);
                    responseType = type instanceof WildCardTypeExpression wildCardTypeExpression
                            ? wildCardTypeExpression.getTypeExpression()
                            : type;
                } else {
                    responseType = new NoTypeExpression(TypeKind.VOID);
                }
            } else if (httpMethod == HttpMethod.POST
                    && StatusCodes.CREATED.equals(okResponse.getKey())) {
                final var requestBody = operation.getRequestBody();

                if (requestBody != null) {
                    final var jsonContent = requestBody.getContent().get(ContentTypes.JSON);
                    responseType = getOpenApiTypeUtils().createTypeExpression(jsonContent.getSchema(), openAPI);
                } else {
                    responseType = new ClassOrInterfaceTypeExpression("java.lang.Object");
                }
            } else {
                responseType = new NoTypeExpression(TypeKind.VOID);
            }
        } else {
            responseType = new NoTypeExpression(TypeKind.VOID);
        }

        final var method = classDeclaration.createMethod()
                .returnType(responseType)
                .simpleName(Name.of(operationId));

        createParameters(openAPI, operation, httpMethod)
                .forEach(method::parameter);
    }

    @Override
    protected ClassDeclaration createClass(final String packageName, final Name simpleName) {
        return new ClassDeclaration()
                .simpleName(Name.of(simpleName))
                .kind(ElementKind.INTERFACE)
                .modifier(Modifier.PUBLIC)
                .annotation(new AnnotationExpression("javax.annotation.processing.Generated", LiteralExpression.createStringLiteralExpression(getClass().getName())));
    }

    @Override
    protected String generateClasName(final String path,
                                      final PathItem openApiPath,
                                      final Operation operation) {
        return super.generateClasName(path, openApiPath, operation) + "ServiceApi";
    }
}
