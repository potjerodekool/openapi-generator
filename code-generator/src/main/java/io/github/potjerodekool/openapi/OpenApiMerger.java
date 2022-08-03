package io.github.potjerodekool.openapi;

import io.github.potjerodekool.openapi.util.ParseException;
import com.reprezen.kaizen.oasparser.OpenApiParser;
import com.reprezen.kaizen.oasparser.model3.OpenApi3;
import com.reprezen.kaizen.oasparser.ovl3.OpenApi3Impl;
import com.reprezen.kaizen.oasparser.ovl3.PathImpl;

import java.io.File;

/**
 * Merges openapi files from the paths directory
 * with the main openapi file.
 */
public class OpenApiMerger {

    private static final OpenApiParser parser = new OpenApiParser();

    public static OpenApi3 merge(final File file) {
        final OpenApi3 rootFile;

        try {
            rootFile = (OpenApi3) parser.parse(file);
        } catch (final Exception e) {
            throw new ParseException(e);
        }

        final var dir = file.getParentFile();
        final var pathDir = new File(dir, "paths");

        if (pathDir.exists()) {
            importPaths(pathDir, rootFile);
        }

        return rootFile;
    }

    private static void importPaths(final File dir,
                             final OpenApi3 rootFile) {
        final var files = dir.listFiles();

        if (files != null) {
            for (final File file : files) {
                if (file.isDirectory()) {
                    importPaths(file, rootFile);
                } else if (file.getName().endsWith(".yml") || file.getName().endsWith("yaml")) {
                    importPathsFromFile(file, rootFile);
                }
            }
        }
    }

    private static void importPathsFromFile(final File file,
                                     final OpenApi3 rootFile) {
        try {
            final var openApi = (OpenApi3) parser.parse(file);
            final var creatingRef = ((OpenApi3Impl) openApi)._getCreatingRef();
            final var paths = openApi.getPaths();
            paths.values().stream()
                    .map(path -> (PathImpl) path)
                    .forEach(path -> path._setCreatingRef(creatingRef));

            rootFile.getPaths().putAll(paths);
        } catch (final Exception e) {
            throw new ParseException(e);
        }
    }
}