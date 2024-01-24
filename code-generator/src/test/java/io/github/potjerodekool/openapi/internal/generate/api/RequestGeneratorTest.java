package io.github.potjerodekool.openapi.internal.generate.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.Language;
import io.github.potjerodekool.codegen.io.Filer;
import io.github.potjerodekool.codegen.io.Location;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.InMemoryFileManager;
import io.github.potjerodekool.openapi.internal.generate.Templates;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestGeneratorTest {

    @Test
    void generate() {
        final var config = new GeneratorConfig(
                Language.JAVA,
                "org.some.api",
                Map.of()
        );

        final var filterMock = mock(Filer.class);
        final InMemoryFileManager fileManager = new InMemoryFileManager();

        when(filterMock.createResource(any(), any(), any()))
                .thenAnswer(answer -> fileManager.createResource(
                        answer.getArgument(0),
                        answer.getArgument(1),
                        answer.getArgument(2)
                ));

        final var environmentMock = mock(Environment.class);
        when(environmentMock.getFiler())
                .thenReturn(filterMock);

        final var template = new Templates();

        final var generator = new RequestGenerator(
                config,
                environmentMock,
                template
        );

        generator.generate();

        final var resource = fileManager.getResource(
                Location.SOURCE_OUTPUT,
                "org.some.api",
                "Request.java"
        );

        final var data =new String(fileManager.getData(resource));
        System.out.println(data);
    }
}

/*
request(packageName, generatorName)::= <<
package <packageName>;

import javax.annotation.processing.Generated;
import java.util.Map;

@Generated(value = "<generatorName>")
public interface Request {
	String getParameter(String name);

	String<[><]> getParameterValues(String name);

	Map<String, String<[><]>> getParameterMap();

	String getRemoteAddr();

	String getRemoteHost();

	int getRemotePort();

}

>>
 */