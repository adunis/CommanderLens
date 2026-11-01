package com.example.commanderlens

/**
 * Prevents successive video frames from counting as successive cards.
 * A new name can be accepted immediately after it stabilizes; the SAME name
 * must disappear from the frame for [emptyFramesToUnlock] analyses first.
 */
class ScanGate(
    private val stableFrames: Int = 3,
    private val emptyFramesToUnlock: Int = 3
) {
    private var currentKey: String? = null
    private var currentText: String? = null
    private var streak = 0
    private var blankFrames = 0
    private var lastAcceptedKey: String? = null

    fun observe(candidate: String?): String? {
        val normalized = candidate?.let(::normalizeName).orEmpty()
        if (normalized.isEmpty()) {
            blankFrames++
            streak = 0
            currentKey = null
            currentText = null
            if (blankFrames >= emptyFramesToUnlock) lastAcceptedKey = null
            return null
        }

        blankFrames = 0
        if (normalized == currentKey) {
            streak++
        } else {
            currentKey = normalized
            currentText = candidate
            streak = 1
        }
        return if (streak >= stableFrames && currentKey != lastAcceptedKey) currentText else null
    }

    fun accepted(candidate: String) {
        lastAcceptedKey = normalizeName(candidate)
    }

    fun reset() {
        currentKey = null
        currentText = null
        streak = 0
        blankFrames = 0
        lastAcceptedKey = null
    }
}

fun normalizeName(name: String): String = name.trim()
    .lowercase(java.util.Locale.ROOT)
    .replace('’', '\'')
    .replace(Regex("\\s+"), " ")
