package io.github.potjerodekool.openapi.internal.generate.service;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.element.Modifier;
import io.github.potjerodekool.codegen.model.element.Name;
import io.github.potjerodekool.codegen.model.tree.AnnotationExpression;
import io.github.potjerodekool.codegen.model.tree.expression.Expression;
import io.github.potjerodekool.codegen.model.tree.expression.LiteralExpression;
import io.github.potjerodekool.codegen.model.tree.statement.java.JClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.java.JVariableDeclaration;
import io.github.potjerodekool.codegen.model.tree.type.ClassOrInterfaceTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.NoTypeExpression;
import io.github.potjerodekool.codegen.model.tree.type.WildCardTypeExpression;
import io.github.potjerodekool.codegen.model.type.TypeKind;
import io.github.potjerodekool.openapi.ApiConfiguration;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.HttpMethod;
import io.github.potjerodekool.openapi.MissingOperationIdException;
import io.github.potjerodekool.openapi.internal.StatusCodes;
import io.github.potjerodekool.openapi.internal.generate.AbstractGenerator;
import io.github.potjerodekool.openapi.internal.generate.ContentTypes;
import io.github.potjerodekool.openapi.internal.generate.OpenApiUtils;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.tree.OpenApiOperation;
import io.github.potjerodekool.openapi.tree.OpenApiPath;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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
    protected List<JVariableDeclaration> createParameters(final OpenApiOperation operation,
                                                          final HttpMethod httpMethod) {
        final var parameters = new ArrayList<>(super.createParameters(operation, httpMethod));

        final var requestParameter = JVariableDeclaration.parameter()
                .modifier(Modifier.FINAL)
                .varType(new ClassOrInterfaceTypeExpression(basePackageName + ".Request"))
                .name("request");
        parameters.add(requestParameter);
        return parameters;
    }

    @Override
    protected void processOperation(final OpenApiPath openApiPath,
                                    final HttpMethod httpMethod,
                                    final String path,
                                    @Nullable final OpenApiOperation operation) {
        if (operation == null) {
            return;
        }

        final var operationId = operation.operationId();

        if (operationId == null
                || operationId.isEmpty()) {
            throw new MissingOperationIdException(path, httpMethod);
        }

        final var classDeclaration = findOrCreateClassDeclaration(openApiPath, operation);

        final var okReponseOptional = OpenApiUtils.findOkResponse(operation.responses());

        final Expression responseType;

        if (okReponseOptional.isPresent()) {
            final var okResponse = okReponseOptional.get();

            final var hasContent = !okResponse.getValue().contentMediaType().isEmpty();

            if (hasContent) {
                final var okResponseType = OpenApiUtils.resolveResponseMediaType(okResponse.getValue().contentMediaType());

                if (okResponseType != null) {
                    final var type = getOpenApiTypeUtils().createTypeExpression(okResponseType);
                    responseType = type instanceof WildCardTypeExpression wildCardTypeExpression
                            ? wildCardTypeExpression.getTypeExpression()
                            : type;
                } else {
                    responseType = new NoTypeExpression(TypeKind.VOID);
                }
            } else if (httpMethod == HttpMethod.POST
                    && StatusCodes.CREATED.equals(okResponse.getKey())) {
                final var requestBody = operation.requestBody();

                if (requestBody != null) {
                    final var jsonContent = requestBody.contentMediaType().get(ContentTypes.JSON);
                    responseType = getOpenApiTypeUtils().createTypeExpression(jsonContent.schema());
                } else {
                    responseType = new ClassOrInterfaceTypeExpression("java.lang.Object");
                }
            } else {
                responseType = new NoTypeExpression(TypeKind.VOID);
            }
        } else {
            responseType = new NoTypeExpression(TypeKind.VOID);
        }

        final var method = classDeclaration.addMethod(responseType, operationId, Set.of());

        createParameters(operation, httpMethod)
                .forEach(method::addParameter);
    }

    @Override
    protected JClassDeclaration createClass(final String packageName, final Name simpleName) {
        return new JClassDeclaration(simpleName, ElementKind.INTERFACE)
                .modifier(Modifier.PUBLIC)
                .annotation(new AnnotationExpression("javax.annotation.processing.Generated", LiteralExpression.createStringLiteralExpression(getClass().getName())));
    }

    @Override
    protected String generateClasName(final OpenApiPath openApiPath, final OpenApiOperation operation) {
        return super.generateClasName(openApiPath, operation) + "ServiceApi";
    }
}
