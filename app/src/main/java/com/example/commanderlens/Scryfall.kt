package com.example.commanderlens

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap

/** Uses Scryfall's fuzzy name lookup, never queries it once per camera frame. */
class Scryfall {
    data class Card(val name: String, val typeLine: String, val id: String)
    sealed interface Result {
        data class Found(val card: Card) : Result
        data class Error(val reason: String) : Result
    }

    private val cache = ConcurrentHashMap<String, Card>()

    fun findByName(ocrName: String): Result {
        val query = normalizeName(ocrName)
        cache[query]?.let { return Result.Found(it) }
        return try {
            val conn = (URL("https://api.scryfall.com/cards/named?fuzzy=" +
                URLEncoder.encode(ocrName, "UTF-8")).openConnection() as HttpURLConnection)
            try {
            conn.requestMethod = "GET"
            conn.connectTimeout = 8_000
            conn.readTimeout = 8_000
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("User-Agent", "CommanderLens/0.1 (personal MTG deck scanner)")
            when (val status = conn.responseCode) {
                200 -> {
                    val data = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                    val name = data.getString("name")
                    // Avoid accepting unrelated fuzzy matches for an OCR fragment.
                    if (!plausibleCardName(ocrName, name)) {
                        Result.Error("Uncertain OCR: $ocrName. Try holding the card steady or add manually.")
                    } else {
                        val card = Card(name, data.optString("type_line"), data.getString("id"))
                        cache[query] = card
                        Result.Found(card)
                    }
                }
                404 -> Result.Error("No match for '$ocrName' — adjust the card or search manually.")
                429 -> Result.Error("Scryfall rate-limited requests. Pause scanning briefly.")
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
