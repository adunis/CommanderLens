package com.example.commanderlens

/** A custom two-copy setting is supported; official singleton Commander uses 1. */
object DeckRules {
    fun isBasicLand(typeLine: String): Boolean =
        Regex("\\bBasic\\b", RegexOption.IGNORE_CASE).containsMatchIn(typeLine) &&
        Regex("\\bLand\\b", RegexOption.IGNORE_CASE).containsMatchIn(typeLine)

    fun mayAdd(existing: Int, isBasicLand: Boolean, maxNonBasicCopies: Int): Boolean =
        isBasicLand || existing < maxNonBasicCopies
}
