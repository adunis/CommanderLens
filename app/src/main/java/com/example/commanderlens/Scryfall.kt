package com.example.commanderlens

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/** Resolve localized printed names locally, then retrieve canonical English card details. */
class Scryfall(private val context: Context) {
    data class Card(val name: String, val typeLine: String, val id: String)
    sealed interface Result {
        data class Found(val card: Card) : Result
        data class Error(val reason: String) : Result
    }

    private val cache = ConcurrentHashMap<String, Card>()
    private val localizedIndex by lazy {
        try {
            context.assets.open("localized_names.tsv").bufferedReader(Charsets.UTF_8).use {
                LocalizedNameIndex.fromTsv(it)
            }
        } catch (e: Exception) {
            android.util.Log.e("CommanderLens", "Italian/Spanish index unavailable", e)
            null
        }
    }

    /** language = auto, en, it or es. Export always uses the Scryfall English name. */
    fun findByName(ocrName: String, language: String = "auto"): Result {
        val printedName = ocrName.trim()
        if (printedName.isBlank()) return Result.Error("Please scan or enter a card name.")
        val cacheKey = "$language:${normalizePrintedName(printedName)}"
        cache[cacheKey]?.let { return Result.Found(it) }

        val translated = if (language == "en") null else localizedIndex?.resolve(printedName, language)
        val lookupName = translated?.englishName ?: printedName
        val result = requestCanonicalCard(lookupName)
        if (result is Result.Found) cache[cacheKey] = result.card
        return result
    }

    private fun requestCanonicalCard(name: String): Result {
        return try {
            val conn = (URL("https://api.scryfall.com/cards/named?fuzzy=" +
                URLEncoder.encode(name, "UTF-8")).openConnection() as HttpURLConnection)
            try {
                conn.requestMethod = "GET"
                conn.connectTimeout = 8_000
                conn.readTimeout = 8_000
                conn.setRequestProperty("Accept", "application/json")
                conn.setRequestProperty("User-Agent", "CommanderLens/0.2 (personal MTG deck scanner)")
                when (val status = conn.responseCode) {
                    200 -> {
                        val data = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                        val cardName = data.getString("name")
                        if (!plausibleCardName(name, cardName)) {
                            Result.Error("Uncertain name: $name. Hold the card steady or enter it manually.")
                        } else {
                            Result.Found(Card(cardName, data.optString("type_line"), data.getString("id")))
                        }
                    }
                    404 -> Result.Error("No match for '$name' — try another angle or enter it manually.")
                    429 -> Result.Error("Scryfall is rate limiting requests. Please pause scanning.")
                    else -> Result.Error("Scryfall returned HTTP $status.")
                }
            } finally {
                conn.disconnect()
            }
        } catch (e: Exception) {
            Result.Error("Network problem: ${e.localizedMessage ?: "couldn't reach Scryfall"}")
        }
    }
}
