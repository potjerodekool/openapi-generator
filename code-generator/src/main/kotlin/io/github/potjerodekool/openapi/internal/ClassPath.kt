package io.github.potjerodekool.openapi.internal

import io.github.potjerodekool.codegen.loader.java.ClassPath
import io.github.potjerodekool.openapi.common.Project
import io.github.potjerodekool.openapi.common.dependency.Artifact
import java.io.File
import java.net.URL
import java.util.*

object ClassPath {
    fun getFullClassPath(project: Project): Array<URL> {
        val fullClassPath = ArrayList<URL>()
        fullClassPath.addAll(getProjectClassPath(project))
        fullClassPath.addAll(Arrays.stream(ClassPath.getJavaClassPath()).toList())
        return fullClassPath.toArray(arrayOf())
    }


    private fun getProjectClassPath(project: Project): List<URL> {
        return project.dependencyChecker.projectArtifacts.toList()
            .mapNotNull(Artifact::file)
            .map { file: File -> file.toURI().toURL()}
            .toList()
    }
}