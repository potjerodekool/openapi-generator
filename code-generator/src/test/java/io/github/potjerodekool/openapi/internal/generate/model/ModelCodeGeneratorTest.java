package io.github.potjerodekool.openapi.internal.generate.model;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.type.PrimitiveType;
import io.github.potjerodekool.openapi.*;
import io.github.potjerodekool.openapi.dependency.Artifact;
import io.github.potjerodekool.openapi.dependency.DependencyChecker;
import io.github.potjerodekool.openapi.internal.Filer;

import static io.github.potjerodekool.openapi.CompilationUnitAsserter.assertClass;

import io.github.potjerodekool.openapi.internal.generate.GenerateUtilsJava;
import io.github.potjerodekool.openapi.internal.generate.TypesJava;
import io.github.potjerodekool.openapi.internal.generate.Types;
import io.github.potjerodekool.openapi.tree.*;
import io.github.potjerodekool.openapi.type.OpenApiStandardType;
import io.github.potjerodekool.openapi.type.OpenApiStandardTypeEnum;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.potjerodekool.openapi.type.OpenApiType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.Mockito.verify;

class ModelCodeGeneratorTest {

    private final Types types = new TypesJava();
    private final Filer filter = Mockito.mock(Filer.class);

    @SuppressWarnings("method.invocation")
    private final OpenApiGeneratorConfig config = config();

    private final DependencyChecker dependencyCheckerMock = new DependencyChecker() {
        @Override
        public boolean isDependencyPresent(String groupId, String artifactId) {
            return false;
        }

        @Override
        public Stream<Artifact> getProjectArtifacts() {
            return Stream.empty();
        }
    };

    private final ModelCodeGenerator generator = new ModelCodeGenerator(
            config,
            types,
            dependencyCheckerMock,
            new GenerateUtilsJava(types),
            filter
    );

    private OpenApiGeneratorConfig config() {
        return OpenApiGeneratorConfig.createBuilder(
                new File("test.yaml"),
                new File("target/ut-generated-sources"),
                "org.some.config"
        ).build();
    }

    private OpenApiInfo createInfo() {
        return new OpenApiInfo(
                "",
                "",
                "",
                new OpenApiContact("", "", "", Map.of()),
                new OpenApiLicense("", "", Map.of()),
                "",
                Map.of()
                );
    }

    private OpenApiPath createPath(final HttpMethod httpMethod,
                                   final String requestPath,
                                   final OpenApiOperation operation) {
        OpenApiOperation post = null;
        OpenApiOperation get = null;
        OpenApiOperation put = null;
        OpenApiOperation patch = null;
        OpenApiOperation delete = null;

        switch (httpMethod) {
            case POST -> post = operation;
            case GET -> get = operation;
            case PUT -> put = operation;
            case PATCH -> patch = operation;
            case DELETE -> delete = operation;
        }

        return new OpenApiPath(
                requestPath,
                post,
                get,
                put,
                patch,
                delete,
                ""
        );
    }

    private OpenApiResponse createResponse(final OpenApiType responseType) {
        return new OpenApiResponse("", Map.of("application/json", new OpenApiContent(responseType, Map.of())), Map.of());
    }

    @Test
    void checkIfFieldIsPrimitiveIntAndFinal() throws IOException {
        final var responseType = new ObjectBuilder()
                .name("user")
                .property("id", new OpenApiStandardType(OpenApiStandardTypeEnum.INTEGER, null, false), true, true, null)
                .build();

        final var response = createResponse(responseType);
        final var path = createPath(
                HttpMethod.GET,
                "/",
                new OpenApiOperation("", "", "get", List.of(), List.of(), null, Map.of("200", response), null));

        final var openApi = new OpenApi(createInfo(), List.of(path), Map.of(), List.of());

        generator.generate(openApi);

        final var captor = ArgumentCaptor.forClass(CompilationUnit.class);
        verify(filter).write(captor.capture());
        final var cu = captor.getValue();

        assertClass(cu, "User")
                .assertField("id", fieldAsserter -> {
                    assertTrue(fieldAsserter.fieldDeclaration().hasModifier(Modifier.Keyword.FINAL));
                    assertEquals(PrimitiveType.intType(), fieldAsserter.fieldDeclaration().getVariable(0).getType());
                });
    }

}