package io.github.potjerodekool.openapi.internal;

import java.io.*;

public interface FileObject {

    String getName();

    Kind getKind();

    long getLastModified();

    InputStream openInputStream() throws IOException;

    OutputStream openOutputStream() throws IOException;

    Reader openReader(boolean ignoreEncodingErrors) throws IOException;

    CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException;

    Writer openWriter() throws IOException;

    enum Kind {
        PROPERTIES,
        YAML,
        UNKNOWN
    }
}
