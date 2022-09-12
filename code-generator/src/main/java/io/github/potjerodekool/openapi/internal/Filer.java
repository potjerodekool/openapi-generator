package io.github.potjerodekool.openapi.internal;

import io.github.potjerodekool.openapi.Language;
import io.github.potjerodekool.openapi.OpenApiGeneratorConfig;
import io.github.potjerodekool.openapi.internal.ast.*;
import io.github.potjerodekool.openapi.internal.util.Utils;
import io.github.potjerodekool.openapi.log.Logger;

import java.io.*;

public class Filer {

    private static final Logger LOGGER = Logger.getLogger(Filer.class.getName());

    private final OpenApiGeneratorConfig config;

    private final TypeUtils typeUtils;

    public Filer(final OpenApiGeneratorConfig config,
                 final TypeUtils typeUtils) {
        this.config = config;
        this.typeUtils = typeUtils;
    }

    public void write(final io.github.potjerodekool.openapi.internal.ast.CompilationUnit cu,
                      final Language language) throws IOException {
        var outputDir = Utils.requireNonNull(config.getOutputDir());

        final var packageElement = cu.getPackageElement();

        if (!packageElement.isDefaultPackage()) {
            final var packageDir = packageElement.getQualifiedName()
                    .replace('.', '/');
            outputDir = new File(outputDir, packageDir);
        }

        write(cu, outputDir, language);
    }

    public void write(final io.github.potjerodekool.openapi.internal.ast.CompilationUnit cu,
                      final File dir,
                      final Language language) throws IOException {

        final io.github.potjerodekool.openapi.internal.ast.CompilationUnit compilationUnit;
        final AbstractAstPrinter astPrinter;
        final var output = new ByteArrayOutputStream();
        final var writer = new BufferedWriter(new OutputStreamWriter(output));
        final var printer = new Printer(writer);

        if (language == Language.KOTLIN) {
            compilationUnit = new JavaToKotlinConverter(typeUtils).convert(cu);
            astPrinter = new KotlinAstPrinter(printer);
        } else {
            compilationUnit = cu;
            astPrinter = new JavaAstPrinter(printer);
        }

        final var unit = new io.github.potjerodekool.openapi.internal.ast.ImportOrganiser().organiseImports(
                compilationUnit
        );

        unit.accept(astPrinter, new CodeContext(unit));
        writer.flush();

        final var typeElement = cu.getElements().get(0);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        final String name = String.format("%s.%s", typeElement.getSimpleName(), language.getFileExtension());
        final var file = new File(dir, name);

        final var code = output.toByteArray();

        try(final var outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
            outputStream.write(code);
        }

        LOGGER.info("Write file " + file.getAbsolutePath());
    }
}
