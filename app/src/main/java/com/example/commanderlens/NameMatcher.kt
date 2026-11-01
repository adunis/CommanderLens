package com.example.commanderlens

/** Allow fuzzy OCR corrections, but prevent short unrelated fragments being accepted as cards. */
fun plausibleCardName(ocrName: String, scryfallName: String): Boolean {
    val query = normalizeName(ocrName)
    // Transforming/double-faced cards are returned by Scryfall as "Front // Back".
    val faces = scryfallName.split("//").map(::normalizeName)
    return faces.any { editSimilarity(query, it) >= 0.72 }
}

private fun editSimilarity(a: String, b: String): Double {
    if (a == b) return 1.0
    if (a.isEmpty() || b.isEmpty()) return 0.0
    val previous = IntArray(b.length + 1) { it }
    val current = IntArray(b.length + 1)
    for (i in 1..a.length) {
        current[0] = i
        for (j in 1..b.length) {
            current[j] = minOf(
                previous[j] + 1,
                current[j - 1] + 1,
                previous[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1
            )
        }
        current.copyInto(previous)
    }
    return 1.0 - previous[b.length].toDouble() / maxOf(a.length, b.length)
}
