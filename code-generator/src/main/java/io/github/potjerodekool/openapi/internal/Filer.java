package io.github.potjerodekool.openapi.internal;

import io.github.potjerodekool.openapi.Language;
import io.github.potjerodekool.openapi.Project;
import io.github.potjerodekool.openapi.internal.ast.*;
import io.github.potjerodekool.openapi.internal.ast.element.PackageElement;
import io.github.potjerodekool.openapi.internal.ast.element.TypeElement;
import io.github.potjerodekool.openapi.internal.ast.util.TypeUtils;
import io.github.potjerodekool.openapi.log.Logger;

import java.io.*;

public class Filer {

    private static final Logger LOGGER = Logger.getLogger(Filer.class.getName());

    private final TypeUtils typeUtils;

    private final FileManager fileManager;

    public Filer(final TypeUtils typeUtils,
                 final Project project) {
        this.typeUtils = typeUtils;
        this.fileManager = new FileManagerImpl(project);
    }

    public void writeSource(final CompilationUnit compilationUnit,
                            final Language language) throws IOException {
        final var clazz = (TypeElement) compilationUnit.getElements().get(0);
        final var packageElement = (PackageElement) clazz.getEnclosingElement();
        final var packageName = packageElement != null
                ? packageElement.getQualifiedName()
                : "";

        final var fileObject = createResource(
                Location.SOURCE_OUTPUT,
                packageName,
                clazz.getSimpleName() + "." + language.getFileExtension()
        );

        try (final var writer = fileObject.openWriter()) {
            final var printer = Printer.create(writer);
            final CompilationUnit cu;
            final AbstractAstPrinter astPrinter;

            if (language == Language.KOTLIN) {
                cu = new JavaToKotlinConverter(typeUtils).convert(compilationUnit);
                astPrinter = new KotlinAstPrinter(printer, typeUtils);
            } else {
                cu = compilationUnit;
                astPrinter = new JavaAstPrinter(printer, typeUtils);
            }

            final var unit = new ImportOrganiser(typeUtils).organiseImports(cu);

            unit.accept(astPrinter, new CodeContext(unit));
            writer.flush();
        }
    }

    public FileObject getResource(final Location location,
                                  final CharSequence moduleAndPkg,
                                  final String relativeName) {
        return fileManager.getResource(location, moduleAndPkg, relativeName);
    }

    public FileObject createResource(final Location location, final CharSequence moduleAndPkg, final String relativeName) {
        return fileManager.createResource(location, moduleAndPkg, relativeName);
    }
}
