package io.github.potjerodekool.openapi.compile.plugin;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public class TestCompiler {
    public byte[] compile(String qualifiedClassName, String testSource) {
        StringWriter output = new StringWriter();

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        SimpleFileManager fileManager = new SimpleFileManager(compiler.getStandardFileManager(
                null,
                null,
                null
        ));
        List<SimpleSourceFile> compilationUnits = singletonList(new SimpleSourceFile(qualifiedClassName, testSource));
        List<String> arguments = new ArrayList<>();
        arguments.addAll(asList("-classpath", System.getProperty("java.class.path"),
                "-Xplugin:" + OpenApiPlugin.NAME));
        JavaCompiler.CompilationTask task = compiler.getTask(output,
                fileManager,
                new CompilerListener(),
                arguments,
                null,
                compilationUnits);
        task.call();
        return fileManager.getCompiled().iterator().next().getCompiledBinaries();
    }
}

class CompilerListener implements DiagnosticListener {

    @Override
    public void report(final Diagnostic diagnostic) {
        System.out.println(diagnostic);
    }
}