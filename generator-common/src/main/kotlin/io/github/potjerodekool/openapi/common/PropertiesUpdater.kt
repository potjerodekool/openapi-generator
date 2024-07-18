package io.github.potjerodekool.openapi.common

import io.github.potjerodekool.codegen.io.FileObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

object PropertiesUpdater {
    fun update(
        fileObject: FileObject,
        updates: Map<String, Any>
    ) {
        try {
            fileObject.openInputStream().use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream, StandardCharsets.ISO_8859_1))
                val tempFile = Files.createTempFile(null, null)
                try {
                    writeTempFile(reader, tempFile, updates)
                    updateProperties(tempFile, fileObject)
                } finally {
                    Files.delete(tempFile)
                }
            }
        } catch (ignored: IOException) {
            //Ignore
        }
    }

    @Throws(IOException::class)
    private fun writeTempFile(
        reader: BufferedReader,
        tempFile: Path,
        updates: Map<String, Any>
    ) {
        val remainingUpdates = HashMap(updates)

        Files.newBufferedWriter(tempFile, StandardCharsets.ISO_8859_1).use { tmpWriter ->
            var line: String
            while ((reader.readLine().also { line = it }) != null) {
                if (line.startsWith("#")) {
                    tmpWriter.write(line)
                    tmpWriter.newLine()
                } else {
                    val keyValue = line.split("=".toRegex(), limit = 2).toTypedArray()
                    val key = keyValue[0]
                    val value: Any?

                    if (remainingUpdates.containsKey(key)) {
                        value = remainingUpdates[key]
                        remainingUpdates.remove(key)
                    } else {
                        value = keyValue[0]
                    }

                    if (key.trim { it <= ' ' }.isNotEmpty()) {
                        tmpWriter.write("$key=$value")
                        tmpWriter.newLine()
                    } else {
                        tmpWriter.write(line)
                        tmpWriter.newLine()
                    }
                }
            }
            for ((key, value) in remainingUpdates) {
                tmpWriter.write("$key= $value")
                tmpWriter.newLine()
            }
        }
    }

    @Throws(IOException::class)
    private fun updateProperties(
        tempFile: Path,
        fileObject: FileObject
    ) {
        Files.newInputStream(tempFile).use { inputStream ->
            val bytes = inputStream.readAllBytes()
            fileObject.writeToOutputStream(bytes)
        }
    }
}
