package io.github.potjerodekool.openapi.internal;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

public class Printer {

    private int tabs = 0;

    private final BufferedWriter writer;

    private final boolean autoFlush;

    public static Printer create(final Writer writer) {
        return create(writer, false);
    }

    public static Printer create(final Writer writer,
                          final boolean autoFlush) {
        final BufferedWriter bufferedWriter =
                writer instanceof BufferedWriter bw
                        ? bw
                        : new BufferedWriter(writer);
        return new Printer(bufferedWriter, autoFlush);
    }

    public Printer(final BufferedWriter writer) {
        this(writer, false);
    }

    public Printer(final BufferedWriter writer,
                   final boolean autoFlush) {
        this.writer = writer;
        this.autoFlush = autoFlush;
    }

    public void indent() {
        this.tabs++;
    }

    public void deIndent() {
        if (this.tabs > 0) {
            this.tabs--;
        }
    }

    public Printer print(final String text) {
        try {
            this.writer.write(text);
            return this;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Printer printLn(final String text) {
        this.print(text).printLn();
        return this;
    }

    public Printer printLn() {
        try {
            this.writer.write("\n");

            if (autoFlush) {
                this.writer.flush();
            }

            return this;
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Printer printIndent() {
        for (int i = 0; i < tabs; i++) {
            print("\t");
        }
        return this;
    }
}
