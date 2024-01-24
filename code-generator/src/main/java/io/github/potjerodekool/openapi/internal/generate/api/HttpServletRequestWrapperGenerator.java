package io.github.potjerodekool.openapi.internal.generate.api;

import io.github.potjerodekool.codegen.Environment;
import io.github.potjerodekool.codegen.io.Filer;
import io.github.potjerodekool.codegen.io.Location;
import io.github.potjerodekool.openapi.GeneratorConfig;
import io.github.potjerodekool.openapi.internal.generate.Templates;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class HttpServletRequestWrapperGenerator {

    private final String basePackageName;
    private final Templates templates;
    private final Filer filer;

    public HttpServletRequestWrapperGenerator(final GeneratorConfig generatorConfig,
                                              final Environment environment,
                                              final Templates templates) {
        this.basePackageName = generatorConfig.basePackageName();
        this.filer = environment.getFiler();
        this.templates = templates;
    }

    public void generate() {
        final var st = templates.getInstanceOf("/request/httpServletRequestWrapper");

        st.add("packageName", this.basePackageName);
        st.add("generatorName", getClass().getName());
        st.add("date", DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now()));

        final var code = st.render();

        final var resource = filer.createResource(
                Location.SOURCE_OUTPUT,
                this.basePackageName,
                "HttpServletRequestWrapper.java"
        );

        resource.writeToOutputStream(code.getBytes());
    }
}
