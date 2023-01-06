package io.github.potjerodekool.openapi.internal;

import io.github.potjerodekool.openapi.Project;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.StringJoiner;

public class FileManagerImpl implements FileManager {

    private final Project project;

    public FileManagerImpl(final Project project) {
        this.project = project;
    }

    @Override
    public FileObject getResource(final Location location,
                                  final CharSequence moduleAndPkg,
                                  final String relativeName) {
        final var fileName = resolveName(moduleAndPkg, relativeName);
        final var paths = resolvePaths(location, resolveFileExtension(fileName));

        if (paths.isEmpty()) {
            return null;
        }

        final var resolvedPathOptional = paths.stream()
                .map(path -> path.resolve(fileName))
                .filter(Files::exists)
                .findFirst();

        if (resolvedPathOptional.isEmpty()) {
            final var path = paths.get(0).resolve(fileName);
            return new PathFileObject(
                    path,
                    resolveKind(fileName)
            );
        }

        final var resolvedPath = resolvedPathOptional.get();
        return new PathFileObject(resolvedPath, resolveKind(fileName));
    }

    @Override
    public FileObject createResource(final Location location,
                                     final CharSequence moduleAndPkg,
                                     final String relativeName) {
        final var fileName = resolveName(moduleAndPkg, relativeName);
        final var paths = resolvePaths(location, resolveFileExtension(fileName));

        if (paths.isEmpty()) {
            return null;
        }

        final var path = paths.get(0).resolve(fileName);
        final var parentPath = path.getParent();

        if (!Files.exists(parentPath)) {
            try {
                Files.createDirectories(parentPath);
            } catch (final IOException e) {
                throw new RuntimeException(e);
            }
        }

        return new PathFileObject(path, resolveKind(fileName));
    }

    private String resolveName(final CharSequence moduleAndPkg,
                               final String relativeName) {
        final String fileName;

        if (moduleAndPkg != null) {
            fileName = moduleAndPkg.toString().replace(".", "/") + "/" +  relativeName;
        } else {
            fileName = relativeName;
        }
        return fileName;
    }

    private String resolveFileExtension(final String fileName) {
        final var sepIndex = fileName.lastIndexOf('.');
        return sepIndex > -1
                ? fileName.substring(sepIndex + 1)
                : "";
    }

    private List<Path> resolvePaths(final Location location,
                                    final String fileExtension) {
        final List<Path> paths;

        if (location == Location.RESOURCE_PATH) {
            paths = project.resourcePaths();
        } else if (location == Location.RESOURCE_OUTPUT) {
            return List.of(project.generatedSourcesDirectory().resolve("resources"));
        } else if (location == Location.SOURCE_OUTPUT) {
            final var subDirectory = switch (fileExtension) {
                case "java" -> "java";
                case "kt" -> "kotlin";
                default -> "";
            };

            if ("".equals(subDirectory)) {
                return List.of(project.generatedSourcesDirectory());
            } else {
                return List.of(project.generatedSourcesDirectory().resolve(subDirectory));
            }
        } else {
            paths = List.of();
        }

        return paths;
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
}

class PathFileObject implements FileObject {

    private final Path path;
    private final Kind kind;

    PathFileObject(final Path path,
                   final Kind kind) {
        this.path = path;
        this.kind = kind;
    }

    @Override
    public String getName() {
        final var separator = path.getFileSystem().getSeparator();
        final var nameJoiner = new StringJoiner(separator);
        final var nameCount = path.getNameCount();

        for (var nameIndex = 0; nameIndex < nameCount; nameIndex++) {
            final var subName = path.getName(nameIndex);
            nameJoiner.add(subName.toString());
        }

        return nameJoiner.toString();
    }

    @Override
    public Kind getKind() {
        return kind;
    }

    @Override
    public long getLastModified() {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (final IOException e) {
            return 0;
        }
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return Files.newInputStream(this.path);
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return Files.newOutputStream(path);
    }

    @Override
    public Reader openReader(final boolean ignoreEncodingErrors) throws IOException {
        return Files.newBufferedReader(path);
    }

    @Override
    public CharSequence getCharContent(final boolean ignoreEncodingErrors) throws IOException {
        try (final InputStream in = openInputStream()) {
            return new String(in.readAllBytes());
        }
    }

    @Override
    public Writer openWriter() throws IOException {
        return Files.newBufferedWriter(path,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
    }
}