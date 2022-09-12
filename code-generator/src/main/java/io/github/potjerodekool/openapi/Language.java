package io.github.potjerodekool.openapi;

public enum Language {

    JAVA("java"),
    KOTLIN("kt");

    private final String fileExtension;

    Language(final String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getFileExtension() {
        return fileExtension;
    }
}
