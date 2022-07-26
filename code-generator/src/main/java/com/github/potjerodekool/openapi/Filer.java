package com.github.potjerodekool.openapi;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.printer.configuration.DefaultPrinterConfiguration;
import com.github.javaparser.printer.configuration.PrinterConfiguration;
import com.github.potjerodekool.openapi.util.Utils;

import java.io.*;

public class Filer {

    private static final Object ARG = new Object();

    private static final Logger LOGGER = Logger.getLogger(Filer.class.getName());

    private final PrinterConfiguration printerConfiguration = new DefaultPrinterConfiguration();

    private final OpenApiGeneratorConfig config;

    public Filer(final OpenApiGeneratorConfig config) {
        this.config = config;
    }

    public void write(final CompilationUnit cu) throws IOException {
        var outputDir = Utils.requireNonNull(config.getOutputDir());

        final var packageDeclarationOptional = cu.getPackageDeclaration();

        if (packageDeclarationOptional.isPresent()) {
            final var packageDir = packageDeclarationOptional.get().getName().toString()
                    .replace('.', '/');
            outputDir = new File(outputDir, packageDir);
        }
        write(outputDir, cu);
    }

    private void write(final File dir,
                       final CompilationUnit cu) throws IOException {

        cu.accept(new ImportOrganiser(), ARG);

        if (!dir.exists()) {
            dir.mkdirs();
        }

        final var type = cu.getTypes().get(0);

        final var code = cu.toString(printerConfiguration);
        final String name = type.getNameAsString() + ".java";
        final var file = new File(dir, name);

        try(final var outputStream = new BufferedOutputStream(new FileOutputStream(file))) {
            outputStream.write(code.getBytes());
        }

        LOGGER.info("Write file " + file.getAbsolutePath());
    }
}
