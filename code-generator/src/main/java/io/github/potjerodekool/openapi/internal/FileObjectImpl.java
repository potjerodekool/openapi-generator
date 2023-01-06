package io.github.potjerodekool.openapi.internal;

import java.io.*;

public class FileObjectImpl implements FileObject {
    @Override
    public String getName() {
        return null;
    }

    @Override
    public Kind getKind() {
        return null;
    }

    @Override
    public long getLastModified() {
        return 0;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return null;
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return null;
    }

    @Override
    public Reader openReader(final boolean ignoreEncodingErrors) throws IOException {
        return null;
    }

    @Override
    public CharSequence getCharContent(final boolean ignoreEncodingErrors) throws IOException {
        return null;
    }

    @Override
    public Writer openWriter() throws IOException {
        return null;
    }
}
