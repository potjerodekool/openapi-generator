package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.model.element.ElementKind;
import io.github.potjerodekool.codegen.model.tree.java.JMethodDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.java.JClassDeclaration;
import io.github.potjerodekool.codegen.model.tree.statement.java.JVariableDeclaration;
import io.github.potjerodekool.codegen.model.util.type.Types;
import io.github.potjerodekool.openapi.ClassPath;
import io.github.potjerodekool.openapi.Project;
import io.github.potjerodekool.openapi.dependency.Artifact;
import io.github.potjerodekool.openapi.dependency.DependencyChecker;
import io.github.potjerodekool.openapi.internal.di.ApplicationContext;
import io.github.potjerodekool.openapi.internal.generate.springmvc.OpenApiTypeUtilsSpringImpl;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class
)
class ModelCodeGeneratorTest {

    private ModelCodeGenerator modelCodeGenerator;

    @Mock
    private Types types;

    @Mock
    private ApplicationContext applicationContext;

    private Environment environment;

    @BeforeEach
    void setup() {
        final var dependencyChecker = mock(DependencyChecker.class);
        final var artifacts = new ArrayList<Artifact>();

        when(dependencyChecker.getProjectArtifacts())
                .thenReturn(artifacts.stream());

        final var project = mock(Project.class);
        when(project.dependencyChecker())
                .thenReturn(dependencyChecker);

        environment = new Environment(ClassPath.getFullClassPath(project));
        final var elements = environment.getElementUtils();

        OpenApiTypeUtils openApiTypeUtils = new OpenApiTypeUtilsSpringImpl();

        modelCodeGenerator = new ModelCodeGenerator(
                openApiTypeUtils,
                environment,
                applicationContext
        );
    }

    @Test
    void test() {
        /*
        final var properties = new HashMap<String, OpenApiSchema<?>>();

        properties.put("code", new OpenApiSchema<>(
                new OpenApiStandardType(
                        OpenApiStandardTypeEnum.INTEGER,
                        "int32",
                        false
                ),
                "int32",
                true,
                List.of(),
                true,
                null,
                null,
                null,
                Map.of(),
                null
        ));

        properties.put("message", new OpenApiSchema<>(
                new OpenApiStandardType(
                        OpenApiStandardTypeEnum.STRING,
                        null,
                        true
                ),
                null,
                true,
                List.of(),
                true,
                null,
                null,
                new Constraints(),
                Map.of(),
                null
        ));

        final var schema = new OpenApiObjectType(
                "ErrorDto",
                List.of("code"),
                properties,
                null
        );

        modelCodeGenerator.generateSchemaModel("ErrorDto", schema, false);

        final var cu = environment.getCompilationUnits().get(0);

        final var astPrinter = new AstPrinter();
        cu.accept(astPrinter, null);
        final var actual = astPrinter.getCode();

        System.out.println(actual);

        final var expected = loadAsString("modelcodegenerator/test.java", true);
        assertEquals(expected, actual);
        */
    }

    private List<JVariableDeclaration> fields(final JClassDeclaration classDeclaration) {
        return classDeclaration.getEnclosed().stream()
                .filter(enclosed -> enclosed instanceof JVariableDeclaration variableDeclaration
                    && variableDeclaration.getKind() == ElementKind.FIELD)
                .map(enclosed -> (JVariableDeclaration) enclosed)
                .toList();
    }

    private List<JMethodDeclaration> constructors(final JClassDeclaration classDeclaration) {
        return classDeclaration.getEnclosed().stream()
                .filter(enclosed -> enclosed instanceof JMethodDeclaration methodDeclaration
                        && methodDeclaration.getKind() == ElementKind.CONSTRUCTOR)
                .map(enclosed -> (JMethodDeclaration) enclosed)
                .toList();
    }
}