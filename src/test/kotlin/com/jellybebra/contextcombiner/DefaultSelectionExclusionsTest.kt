package com.jellybebra.contextcombiner

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultSelectionExclusionsTest {
    @Test
    fun `excludes dependency lock and checksum files`() {
        listOf(
            "go.sum",
            "go.work.sum",
            "package-lock.json",
            "pnpm-lock.yaml",
            "yarn.lock",
            "Cargo.lock",
            "poetry.lock"
        ).forEach { fileName ->
            assertTrue("Expected $fileName to be excluded", DefaultSelectionExclusions.matches(fileName))
        }
    }

    @Test
    fun `keeps dependency manifests and source files selected`() {
        listOf(
            "go.mod",
            "package.json",
            "Cargo.toml",
            "build.gradle.kts",
            "main.go"
        ).forEach { fileName ->
            assertFalse("Expected $fileName to stay selected", DefaultSelectionExclusions.matches(fileName))
        }
    }

    @Test
    fun `matches file names case insensitively`() {
        assertTrue(DefaultSelectionExclusions.matches("PACKAGE-LOCK.JSON"))
    }
}
