package io.github.potjerodekool.openapi;

import io.github.potjerodekool.openapi.internal.OpenApiGeneratorConfigImpl;
import io.github.potjerodekool.openapi.internal.OpenApiParserHelper;
import io.github.potjerodekool.openapi.internal.TreeBuilder;
import org.junit.jupiter.api.Test;

import java.io.File;

public class OpenApiParseTest {

    @Test
    void test() {
        final var rootDir = new File("src/test/resources");
        final var openApiFile = new File(rootDir, "api.json");
        final var outputDir = new File("target/generated-sources");

        final var openApi = OpenApiParserHelper.parse(openApiFile);
        final var config = new OpenApiGeneratorConfigImpl(
                openApiFile,
                outputDir,
                "org.some.models",
                null,
                Language.JAVA,
                ConfigType.EXTERNAL);

        final var builder = new TreeBuilder(config);
        final var api = builder.build(openApi, rootDir);
        System.out.println(api);
    }

}
