package com.jellybebra.contextcombiner

internal object DefaultSelectionExclusions {
    private val excludedFileNamePatterns: List<Regex> by lazy {
        val resource = checkNotNull(
            DefaultSelectionExclusions::class.java.getResourceAsStream("/default-selection-excludes.txt")
        ) { "Missing default-selection-excludes.txt resource" }

        resource.bufferedReader().useLines { lines ->
            lines
                .map(String::trim)
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .map(::globToRegex)
                .toList()
        }
    }

    fun matches(fileName: String): Boolean {
        return excludedFileNamePatterns.any { pattern -> pattern.matches(fileName) }
    }

    private fun globToRegex(glob: String): Regex {
        val regex = buildString {
            append('^')
            glob.forEach { character ->
                when (character) {
                    '*' -> append(".*")
                    '?' -> append('.')
                    else -> append(Regex.escape(character.toString()))
                }
            }
            append('$')
        }
        return Regex(regex, RegexOption.IGNORE_CASE)
    }
}
