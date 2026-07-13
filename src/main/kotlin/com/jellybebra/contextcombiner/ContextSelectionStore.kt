package com.jellybebra.contextcombiner

import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

internal data class SavedSelection(
    val selectedFiles: Set<String>,
    val fullySelectedDirectories: Set<String>
) {
    fun isFileSelected(relativePath: String): Boolean {
        return relativePath in selectedFiles || fullySelectedDirectories.any { directory ->
            directory.isEmpty() || relativePath.startsWith("$directory/")
        }
    }

    fun isDirectoryFullySelected(relativePath: String): Boolean {
        return relativePath in fullySelectedDirectories
    }
}

internal class ContextSelectionStore(
    project: Project,
    contextFile: VirtualFile
) {
    private val properties = PropertiesComponent.getInstance(project)
    private val propertyKey = "contextCombiner.selection.v1.${hash(contextFile.url)}"

    fun load(): SavedSelection? {
        val values = properties.getValues(propertyKey) ?: return null
        if (FORMAT_MARKER !in values) return null

        return SavedSelection(
            selectedFiles = values.asSequence()
                .filter { it.startsWith(FILE_PREFIX) }
                .map { it.removePrefix(FILE_PREFIX) }
                .toSet(),
            fullySelectedDirectories = values.asSequence()
                .filter { it.startsWith(DIRECTORY_PREFIX) }
                .map { it.removePrefix(DIRECTORY_PREFIX) }
                .toSet()
        )
    }

    fun save(selection: SavedSelection) {
        val values = buildList {
            add(FORMAT_MARKER)
            selection.selectedFiles.sorted().forEach { add("$FILE_PREFIX$it") }
            selection.fullySelectedDirectories.sorted().forEach { add("$DIRECTORY_PREFIX$it") }
        }
        properties.setValues(propertyKey, values.toTypedArray())
    }

    private fun hash(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val FORMAT_MARKER = "format:1"
        const val FILE_PREFIX = "file:"
        const val DIRECTORY_PREFIX = "directory:"
    }
}
