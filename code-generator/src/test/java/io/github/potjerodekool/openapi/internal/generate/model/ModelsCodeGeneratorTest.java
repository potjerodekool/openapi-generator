package io.github.potjerodekool.openapi.internal.generate.model;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.io.Filer;
import io.github.potjerodekool.codegen.loader.asm.ClassPath;
import io.github.potjerodekool.codegen.model.CompilationUnit;
import io.github.potjerodekool.codegen.model.symbol.ClassSymbol;
import io.github.potjerodekool.codegen.model.util.type.Types;
import io.github.potjerodekool.openapi.dependency.DependencyChecker;
import io.github.potjerodekool.openapi.internal.type.OpenApiTypeUtils;
import io.github.potjerodekool.openapi.internal.di.ApplicationContext;
import io.github.potjerodekool.openapi.internal.util.TypeUtils;
import io.github.potjerodekool.openapi.tree.OpenApi;
import io.github.potjerodekool.openapi.tree.OpenApiProperty;
import io.github.potjerodekool.openapi.type.OpenApiObjectType;
import io.github.potjerodekool.openapi.type.OpenApiStandardType;
import io.github.potjerodekool.openapi.type.OpenApiStandardTypeEnum;
import io.github.potjerodekool.openapi.type.OpenApiType;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@Disabled
@ExtendWith(MockitoExtension.class)
class ModelsCodeGeneratorTest {

    private final Environment environment = new Environment(ClassPath.getJavaClassPath());
    private final Types types = environment.getTypes();
    private final TypeUtils typeUtils = new TypeUtils(types, environment.getElementUtils());
    private final OpenApiTypeUtils openApiTypeUtils = new OpenApiTypeUtils(types, typeUtils, environment.getElementUtils());

    @Test
    void generate() throws IOException {
        final var filer = Mockito.mock(Filer.class);
        final var dependencyChecker = Mockito.mock(DependencyChecker.class);
        final var environment = Mockito.mock(Environment.class);

        when(environment.getTypes()).thenReturn(types);
        when(environment.getElementUtils()).thenReturn(null);
        when(environment.getFiler()).thenReturn(filer);

        final var modelGenerator  = new ModelsCodeGenerator(
                typeUtils,
                openApiTypeUtils,
                environment,
                Language.JAVA,
                new ApplicationContext(dependencyChecker)
        );

        final var openApi = Mockito.mock(OpenApi.class);
        final var schemas = new HashMap<String, OpenApiType>();

        final var personProperties = new HashMap<String, OpenApiProperty>();

        final var idProperty = new OpenApiProperty(
                new OpenApiStandardType(
                        OpenApiStandardTypeEnum.INTEGER,
                        null,
                        false
                ),
                true,
                true,
                true,
                null
        );
        personProperties.put("id", idProperty);

        final var personSchema = new OpenApiObjectType(
                "Person",
                personProperties,
                null
        );

        schemas.put("Person", personSchema);

        when(openApi.schemas())
                        .thenReturn(schemas);

        modelGenerator.generate(openApi);

        final var cuCaptor = ArgumentCaptor.forClass(CompilationUnit.class);
        Mockito.verify(filer).writeSource(cuCaptor.capture(), ArgumentMatchers.any());
        final var compilationUnit = cuCaptor.getValue();
        assertNotNull(compilationUnit);

        final var personClass = (ClassSymbol) compilationUnit.getElements().stream()
                .findFirst()
                .get();

        throw new UnsupportedOperationException();
        /*
        new TypeElementAsserter(personClass, types)
                .hasField("id")
                .checkType(types.getPrimitiveType(TypeKind.INT));
        */
    }
}