package io.github.potjerodekool.openapi;

import java.io.File;
import java.util.Map;

public record ApiConfiguration(File apiFile,
                               File schemasDir,
                               File pathsDir,
                               String basePackageName,
                               boolean generateApiDefinitions,
                               boolean generateModels,
                               Map<String, String> controllers) {

    public ApiConfiguration(File apiFile,
                            String basePackageName,
                            boolean generateApiDefinitions,
                            boolean generateModels,
                            Map<String, String> controllers) {
        this(apiFile, createRelativeFile(apiFile, "schemas"), createRelativeFile(apiFile, "paths"), basePackageName, generateApiDefinitions, generateModels, controllers);
    }

    private static File createRelativeFile(final File file,
                                           final String name) {
        return new File(file.getParentFile(), name);
    }

    public String modelPackageName() {
        return basePackageName + ".model";
    }

    public ApiConfiguration withControllers(Map<String, String> controllers) {
        return new ApiConfiguration(
                apiFile,
                basePackageName,
                generateApiDefinitions,
                generateModels,
                controllers
        );
    }
}
