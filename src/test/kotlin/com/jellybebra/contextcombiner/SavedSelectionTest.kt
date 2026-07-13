package com.jellybebra.contextcombiner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SavedSelectionTest {
    @Test
    fun `restores explicitly selected files`() {
        val selection = SavedSelection(
            selectedFiles = setOf("src/selected.kt"),
            fullySelectedDirectories = emptySet()
        )

        assertTrue(selection.isFileSelected("src/selected.kt"))
        assertFalse(selection.isFileSelected("src/new.kt"))
    }

    @Test
    fun `selects new files below fully selected directory`() {
        val selection = SavedSelection(
            selectedFiles = emptySet(),
            fullySelectedDirectories = setOf("src/main")
        )

        assertTrue(selection.isFileSelected("src/main/NewFile.kt"))
        assertTrue(selection.isFileSelected("src/main/nested/NewFile.kt"))
        assertFalse(selection.isFileSelected("src/mainly/NewFile.kt"))
    }

    @Test
    fun `fully selected root includes every new file`() {
        val selection = SavedSelection(
            selectedFiles = emptySet(),
            fullySelectedDirectories = setOf("")
        )

        assertTrue(selection.isFileSelected("new-file.txt"))
        assertTrue(selection.isFileSelected("new-folder/new-file.txt"))
    }
}
