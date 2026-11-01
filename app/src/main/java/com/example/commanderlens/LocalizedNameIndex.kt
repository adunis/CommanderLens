package com.example.commanderlens

import java.io.Reader
import java.text.Normalizer
import java.util.Locale

/** Compact MTGJSON-derived dictionary: printed IT/ES card name -> canonical English name. */
class LocalizedNameIndex private constructor(
    private val exact: Map<String, List<Match>>,
    private val byPrefix: Map<String, List<String>>
) {
    data class Match(val englishName: String, val language: String)

    companion object {
        fun fromTsv(reader: Reader): LocalizedNameIndex {
            val entries = HashMap<String, MutableSet<Match>>()
            reader.buffered().useLines { lines ->
                lines.forEach { line ->
                    val fields = line.split('\t')
                    if (fields.size != 3) return@forEach
                    val language = fields[0]
                    if (language != "it" && language != "es") return@forEach
                    val key = normalizePrintedName(fields[1])
                    if (key.length < 2 || fields[2].isBlank()) return@forEach
                    entries.getOrPut(key) { LinkedHashSet() }.add(Match(fields[2], language))
                }
            }
            val index = entries.mapValues { it.value.toList() }
            return LocalizedNameIndex(index, index.keys.groupBy { it.take(2) })
        }
    }

    /** Auto recognizes both languages. Forced language mode resolves name collisions. */
    fun resolve(ocrName: String, language: String = "auto"): Match? {
        val key = normalizePrintedName(ocrName)
        if (key.length < 2) return null
        val exactHit = uniqueEnglishMatch(exact[key].orEmpty(), language)
        if (exactHit != null) return exactHit
        // If an exact name is ambiguous, never guess based on spelling alone.
        if (exact.containsKey(key)) return null
        if (key.length < 6) return null
        val keys = byPrefix[key.take(2)].orEmpty()
        val ranked = keys.asSequence()
            .filter { kotlin.math.abs(it.length - key.length) <= 2 }
            .map { candidate -> candidate to similarity(key, candidate) }
            .filter { it.second >= 0.87 }
            .sortedByDescending { it.second }
            .take(8)
            .mapNotNull { (candidate, score) ->
                uniqueEnglishMatch(exact[candidate].orEmpty(), language)?.let { it to score }
            }
            .toList()
        val winner = ranked.firstOrNull() ?: return null
        val nextDifferent = ranked.drop(1).firstOrNull { it.first.englishName != winner.first.englishName }
        return if (nextDifferent == null || winner.second - nextDifferent.second >= 0.06) winner.first else null
    }

    private fun uniqueEnglishMatch(candidates: List<Match>, language: String): Match? {
        val filtered = candidates.filter { language == "auto" || it.language == language }
        return if (filtered.map { normalizeName(it.englishName) }.distinct().size == 1) filtered.firstOrNull() else null
    }
}

fun normalizePrintedName(name: String): String =
    Normalizer.normalize(name.trim().lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{M}+"), "")
        .replace('’', '\'')
        .replace(Regex("\\s+"), " ")

/** Levenshtein similarity for minor OCR errors, with a conservative acceptance threshold. */
private fun similarity(a: String, b: String): Double {
    if (a == b) return 1.0
    if (a.isEmpty() || b.isEmpty()) return 0.0
    val previous = IntArray(b.length + 1) { it }
    val current = IntArray(b.length + 1)
    for (i in 1..a.length) {
        current[0] = i
        for (j in 1..b.length) {
            current[j] = minOf(previous[j] + 1, current[j - 1] + 1,
                previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1)
        }
        current.copyInto(previous)
    }
    return 1.0 - previous[b.length].toDouble() / maxOf(a.length, b.length)
}
