package io.github.potjerodekool.openapi.internal;

public interface FileManager {

    FileObject getResource(Location location, CharSequence moduleAndPkg, String relativeName);

    FileObject createResource(Location location, CharSequence moduleAndPkg, String relativeName);
}
