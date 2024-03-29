package io.github.potjerodekool.openapi;

import io.github.potjerodekool.codegen.io.FileManager;
import io.github.potjerodekool.codegen.io.FileObject;
import io.github.potjerodekool.codegen.io.Location;

import java.io.*;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryFileManager implements FileManager {

    private final Map<Location, Map<String, InMemoryFileObject>> files = new HashMap<>();

    @Override
    public FileObject getResource(final Location location, final CharSequence moduleAndPkg, final String relativeName) {
        final var subMap = files.get(location);

        if (subMap == null) {
            return null;
        }

        final String fileName = resolveName(moduleAndPkg, relativeName);
        return subMap.get(fileName);
    }

    @Override
    public FileObject createResource(final Location location, final CharSequence moduleAndPkg, final String relativeName) {
        final var fileName = resolveName(moduleAndPkg, relativeName);
        final var subMap = files.computeIfAbsent(location, (key) -> new HashMap<>());
        final var fileObject = new InMemoryFileObject(
                fileName,
                resolveKind(fileName)
        );
        subMap.put(fileName, fileObject);
        return fileObject;
    }

    @Override
    public void setPathsForLocation(final Location location,
                                    final List<Path> list) {
    }

    private FileObject.Kind resolveKind(final String fileName) {
        if (fileName.endsWith(".properties")) {
            return FileObject.Kind.PROPERTIES;
        } else if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
            return FileObject.Kind.YAML;
        } else {
            return FileObject.Kind.UNKNOWN;
        }
    }

    public void reset() {
        files.clear();
    }

    public void add(final Location location,
                    final String moduleAndPkg,
                    final String relativeName,
                    final byte[] data) {
        final var fileObject = (InMemoryFileObject) createResource(location, moduleAndPkg, relativeName);
        fileObject.setData(data);
    }

    private String resolveName(final CharSequence moduleAndPkg,
                               final String relativeName) {
        final String fileName;

        if (moduleAndPkg != null && !moduleAndPkg.isEmpty()) {
            final var parentName = moduleAndPkg.toString().replace('.', '/');
            fileName = String.format("%s/%s", parentName, relativeName);
        } else {
            fileName = relativeName;
        }
        return fileName;
    }

    public byte[] getData(final FileObject fileObject) {
        if (fileObject instanceof InMemoryFileObject inMemoryFileObject) {
            return inMemoryFileObject.getData();
        } else {
            throw new IllegalArgumentException("not a InMemoryFileObject: " + fileObject);
        }
    }
}

class InMemoryFileObject implements FileObject {

    private final String name;
    private final Kind kind;
    private byte[] data;
    private long lastModified;
    private ByteArrayOutputStream outputStream;

    InMemoryFileObject(final String name,
                       final Kind kind) {
        this(name, kind, new byte[0]);
    }

    InMemoryFileObject(final String name,
                       final Kind kind,
                       final byte[] data) {
        this.name = name;
        this.kind = kind;
        this.data = data;
        this.lastModified = lastModified(data);
    }

    private static long lastModified(final byte[] data) {
        if (data.length == 0) {
            return  0;
        } else {
            return System.currentTimeMillis();
        }
    }

    public void setData(final byte[] data) {
        this.data = data;
        this.lastModified = lastModified(data);
    }

    public byte[] getData() {
        if (outputStream != null) {
            return outputStream.toByteArray();
        } else {
            return data;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public long getLastModified() {
        return lastModified;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        this.outputStream = new ByteArrayOutputStream();
        return this.outputStream;
    }

    @Override
    public Reader openReader(final boolean ignoreEncodingErrors) throws IOException {
        return new StringReader(new String(data));
    }

    @Override
    public CharSequence getCharContent(final boolean ignoreEncodingErrors) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Writer openWriter() throws IOException {
        return new DelegateWriter(new StringWriter(), this);
    }

}

class DelegateWriter extends Writer {

    private final StringWriter writer;
    private final InMemoryFileObject fileObject;

    DelegateWriter(final StringWriter writer,
                   final InMemoryFileObject fileObject) {
        this.writer = writer;
        this.fileObject = fileObject;
    }

    @Override
    public void write(final char[] cbuf, final int off, final int len) throws IOException {
        this.writer.write(cbuf, off, len);
    }

    @Override
    public void flush() throws IOException {
        this.writer.flush();
    }

    @Override
    public void close() throws IOException {
        this.writer.close();
        final var data = this.writer.getBuffer().toString().getBytes();
        this.fileObject.setData(data);
    }
}