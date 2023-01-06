package io.github.potjerodekool.openapi.compile.plugin;

import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;

import com.google.auto.service.AutoService;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.util.Context.Key;

import javax.tools.JavaFileManager;
import java.io.*;

@AutoService(Plugin.class)
public class OpenApiPlugin implements Plugin {

    public static final String NAME = "Open_Api_Plugin";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void init(final JavacTask task, final String... args) {
        try {
            final var context = ((BasicJavacTask) task).getContext();
            final var key = new Key<JavaFileManager>();
            final var fileManager = context.get(key);

            task.addTaskListener(new SimpleTaskListener());

            // ((JavacFileManager) ((JavacTaskImpl) task).fileManager).getLocation(StandardLocation.CLASS_PATH)


            System.out.println("Hello from " + getName());
        } catch (final Throwable e) {
            final var file = new File("errors.log");
            if (file.exists()) {
                file.delete();
            }

            try (final var outputStream = new FileOutputStream(file)) {
                final var stringWriter = new StringWriter();
                final var printWriter = new PrintWriter(stringWriter);
                e.printStackTrace(printWriter);
                printWriter.flush();
                printWriter.close();
                final var error = stringWriter.toString();
                outputStream.write(error.getBytes());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public void started(final TaskEvent e) {

    }

    @Override
    public boolean autoStart() {
        return true;
    }
}

class SimpleTaskListener implements TaskListener {

    private boolean generateCode = true;

    @Override
    public void started(final TaskEvent e) {
        if (e.getKind() == TaskEvent.Kind.COMPILATION) {
            final var file = new File("target/generated-sources");

            if (generateCode) {
                generateCode(file);
            }
        }
    }

    private void generateCode(final File dir) {
        generateCode = false;

        final var code = """
                public class MyGeneratedCode {
                }
                """;

        final var file = new File(dir, "MyGeneratedCode.java");
        if (file.exists()) {
            file.delete();
        }

        try (final var outputStream = new FileOutputStream(file)) {
            outputStream.write(code.getBytes());
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}