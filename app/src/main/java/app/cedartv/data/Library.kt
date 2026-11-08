package app.cedartv.data

import android.content.SharedPreferences
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/** What is remembered about a video/song. Language-independent: the playable file is looked up again by [key]. */
@Serializable
data class Stored(
    val key: String,
    val type: String = "video",
    val title: String = "",
    val thumb: String? = null,
    val duration: Double = 0.0,
)

@Serializable
data class Prog(val item: Stored, val posMs: Long, val durMs: Long, val at: Long)

fun Media.toStored() = Stored(languageAgnosticNaturalKey.ifBlank { naturalKey }, type.ifBlank { "video" }, title, thumb(), duration)

internal fun mmss(sec: Double): String {
    val s = sec.toInt()
    return if (s >= 3600) String.format("%d:%02d:%02d", s / 3600, s % 3600 / 60, s % 60) else String.format("%d:%02d", s / 60, s % 60)
}

/** A card-only Media (no files): enough to draw a card; playing it first resolves the real item by key. */
fun Stored.toMedia() = Media(
    naturalKey = key, languageAgnosticNaturalKey = key, title = title, type = type, duration = duration,
    durationFormattedHHMM = if (duration > 0) mmss(duration) else "",
    images = thumb?.let { t -> buildJsonObject { put("wss", buildJsonObject { put("lg", t) }) } },
)

/** Favorites, history and resume positions, kept on the device. */
class LibraryStore(private val prefs: SharedPreferences) {
    private val json = Json { ignoreUnknownKeys = true }
    private val storedList = ListSerializer(Stored.serializer())
    private val progList = ListSerializer(Prog.serializer())
    private val strList = ListSerializer(String.serializer())

    var favorites by mutableStateOf(read("fav")); private set
    var history by mutableStateOf(read("hist")); private set
    var progress by mutableStateOf(readProg()); private set

    private fun read(k: String): List<Stored> =
        runCatching { json.decodeFromString(storedList, prefs.getString(k, null) ?: "[]") }.getOrDefault(emptyList())

    private fun readProg(): List<Prog> =
        runCatching { json.decodeFromString(progList, prefs.getString("prog", null) ?: "[]") }.getOrDefault(emptyList())

    fun isFav(key: String) = favorites.any { it.key == key }

    /** Returns true if the item is a favorite after the call. */
    fun toggleFav(s: Stored): Boolean {
        val now = !isFav(s.key)
        favorites = if (now) (listOf(s) + favorites).take(500) else favorites.filter { it.key != s.key }
        prefs.edit().putString("fav", json.encodeToString(storedList, favorites)).apply()
        return now
    }

    fun addHistory(s: Stored) {
        history = (listOf(s) + history.filter { it.key != s.key }).take(100)
        prefs.edit().putString("hist", json.encodeToString(storedList, history)).apply()
    }

    fun clearHistory() {
        history = emptyList()
        prefs.edit().putString("hist", "[]").apply()
    }

    /** Remembers the position; forgets it near the start or the end (watched). */
    fun saveProgress(s: Stored, posMs: Long, durMs: Long) {
        if (durMs <= 0) return
        val rest = progress.filter { it.item.key != s.key }
        val keep = posMs > durMs * 0.03 && posMs < durMs * 0.95
        progress = if (keep) listOf(Prog(s, posMs, durMs, System.currentTimeMillis())) + rest else rest
        prefs.edit().putString("prog", json.encodeToString(progList, progress.take(60))).apply()
    }

    /** "Start over": drop the saved position so the video begins at 0:00. */
    fun forget(key: String) {
        progress = progress.filter { it.item.key != key }
        prefs.edit().putString("prog", json.encodeToString(progList, progress.take(60))).apply()
    }

    var recent by mutableStateOf(runCatching { json.decodeFromString(strList, prefs.getString("recent", null) ?: "[]") }.getOrDefault(emptyList())); private set

    fun addRecent(q: String) {
        val t = q.trim(); if (t.length < 2) return
        recent = (listOf(t) + recent.filter { !it.equals(t, true) }).take(8)
        prefs.edit().putString("recent", json.encodeToString(strList, recent)).apply()
    }

    fun clearRecent() { recent = emptyList(); prefs.edit().putString("recent", "[]").apply() }

    /** 0..1 share of the video already watched (0 = none). */
    fun fraction(key: String): Float =
        progress.firstOrNull { it.item.key == key }?.let { if (it.durMs > 0) (it.posMs.toFloat() / it.durMs).coerceIn(0f, 1f) else 0f } ?: 0f

    fun resumeMs(key: String): Long = progress.firstOrNull { it.item.key == key }?.posMs ?: 0L

    fun continueList(): List<Stored> = progress.sortedByDescending { it.at }.take(20).map { it.item }
}
