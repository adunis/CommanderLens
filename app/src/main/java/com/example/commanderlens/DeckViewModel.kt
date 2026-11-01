package com.example.commanderlens

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class DeckEntry(
    val name: String,
    val typeLine: String,
    val id: String,
    val quantity: Int,
) {
    val isBasic: Boolean get() = DeckRules.isBasicLand(typeLine)
}

class DeckViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = app.getSharedPreferences("commanderlens", 0)
    private val api = Scryfall(app.applicationContext)
    val cards = mutableStateListOf<DeckEntry>()
    var maxCopies by mutableStateOf(prefs.getInt("max_copies", 2))
        private set
    var scanLanguage by mutableStateOf(prefs.getString("scan_language", "auto") ?: "auto")
        private set
    var scanning by mutableStateOf(true)
        private set
    var status by mutableStateOf("Ready. Hold a card inside the outline.")
        private set
    var lookingUp by mutableStateOf(false)
        private set
    private var lookupJob: Job? = null

    init {
        val data = prefs.getString("cards", "[]") ?: "[]"
        try {
            val array = JSONArray(data)
            for (i in 0 until array.length()) {
                val row = array.getJSONObject(i)
                cards.add(
                    DeckEntry(
                        row.getString("name"), row.optString("typeLine"),
                        row.optString("id"), row.optInt("quantity", 1).coerceAtLeast(1)
                    )
                )
            }
        } catch (e: Exception) {
            status = "Could not read saved deck; starting with an empty list."
        }
    }

    fun setMaxCopies(value: Int) {
        require(value in 1..2)
        maxCopies = value
        prefs.edit().putInt("max_copies", value).apply()
        status = if (value == 1) "Singleton mode: max 1 nonbasic copy. Remove any existing extras manually."
                 else "Custom mode: max 2 nonbasic copies."
    }

    fun changeScanLanguage(value: String) {
        require(value in listOf("auto", "en", "it", "es"))
        scanLanguage = value
        prefs.edit().putString("scan_language", value).apply()
        status = "Card language: " + when (value) {
            "it" -> "Italiano"
            "es" -> "Español"
            "en" -> "English"
            else -> "Auto (EN/IT/ES)"
        }
    }

    fun setScanning(value: Boolean) {
        scanning = value
        status = if (value) "Scanning resumed." else "Scanning paused."
    }

    /** Called from main-thread ML Kit callbacks; false means retry while the name is visible. */
    fun scanCandidate(rawName: String): Boolean {
        if (!scanning || lookingUp) return false
        lookup(rawName)
        return true
    }

    fun manualAdd(rawName: String) {
        if (rawName.isBlank()) return
        if (lookingUp) {
            status = "Wait for the current lookup to finish."
            return
        }
        lookup(rawName.trim())
    }

    private fun lookup(rawName: String) {
        lookingUp = true
        status = "Identifying: $rawName ..."
        lookupJob = viewModelScope.launch {
            try {
                val language = scanLanguage
                val result = withContext(Dispatchers.IO) { api.findByName(rawName, language) }
                when (result) {
                    is Scryfall.Result.Found -> addResolvedCard(result.card)
                    is Scryfall.Result.Error -> status = result.reason
                }
            } catch (e: Exception) {
                status = "Lookup failed: ${e.localizedMessage ?: "unknown error"}"
            } finally {
                lookingUp = false
            }
        }
    }

    private fun addResolvedCard(card: Scryfall.Card) {
        // Dedupes all reprints by canonical name, NOT by printing ID.
        val index = cards.indexOfFirst { normalizeName(it.name) == normalizeName(card.name) }
        val existing = if (index >= 0) cards[index].quantity else 0
        val basic = DeckRules.isBasicLand(card.typeLine)
        if (!DeckRules.mayAdd(existing, basic, maxCopies)) {
            status = "Limit reached: ${card.name} already has $existing ${if (existing == 1) "copy" else "copies"}."
            return
        }
        if (index >= 0) {
            cards[index] = cards[index].copy(quantity = existing + 1)
        } else {
            cards.add(0, DeckEntry(card.name, card.typeLine, card.id, 1))
        }
        save()
        status = "✓ Added ${card.name} (${totalCards()}/100 cards)"
    }

    fun increment(entry: DeckEntry) {
        val index = cards.indexOfFirst { normalizeName(it.name) == normalizeName(entry.name) }
        if (index < 0) return
        if (!DeckRules.mayAdd(cards[index].quantity, entry.isBasic, maxCopies)) {
            status = "Copy limit reached for ${entry.name}."
            return
        }
        cards[index] = cards[index].copy(quantity = cards[index].quantity + 1)
        save()
    }

    fun decrement(entry: DeckEntry) {
        val index = cards.indexOfFirst { normalizeName(it.name) == normalizeName(entry.name) }
        if (index < 0) return
        if (cards[index].quantity <= 1) cards.removeAt(index)
        else cards[index] = cards[index].copy(quantity = cards[index].quantity - 1)
        save()
        status = "Updated ${entry.name}."
    }

    fun totalCards(): Int = cards.sumOf { it.quantity }
    fun uniqueCards(): Int = cards.size

    fun exportText(): String = cards.sortedWith(compareBy<DeckEntry> { !it.isBasic }.thenBy { it.name })
        .joinToString("\n") { "${it.quantity} ${it.name}" } + "\n"

    private fun save() {
        val json = JSONArray()
        cards.forEach { entry ->
            json.put(
                JSONObject().put("name", entry.name).put("typeLine", entry.typeLine)
                    .put("id", entry.id).put("quantity", entry.quantity)
            )
        }
        prefs.edit().putString("cards", json.toString()).apply()
    }
}
