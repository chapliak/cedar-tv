package app.cedartv.data

import android.app.DownloadManager
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File

/** One download. [id] = "language|key": the same video in another language is a separate file. */
@Serializable
data class Dl(
    val id: String,
    val key: String,
    val lang: String,
    val type: String,
    val title: String,
    val thumb: String? = null,
    val duration: Double = 0.0,
    val file: String,
    val sub: String? = null,
    val dmId: Long,
    val subDmId: Long = -1,
    val status: Int = DownloadManager.STATUS_PENDING,
    val bytes: Long = 0,
    val total: Long = 0,
) {
    val done get() = status == DownloadManager.STATUS_SUCCESSFUL
    val failed get() = status == DownloadManager.STATUS_FAILED
    val pct get() = if (total > 0) (bytes * 100 / total).toInt().coerceIn(0, 100) else 0
}

/** Card for the Downloads row; the duration line shows the state. */
fun Dl.toMedia() = Media(
    naturalKey = id, languageAgnosticNaturalKey = key, title = title, type = type, duration = duration,
    durationFormattedHHMM = when {
        done -> mmss(duration)
        failed -> "!"
        else -> "$pct%"
    },
    images = thumb?.let { t -> buildJsonObject { put("wss", buildJsonObject { put("lg", t) }) } },
)

/**
 * Offline copies in the app's own folder (removed with the app). The transfer itself is done by
 * Android's DownloadManager, so it continues in the background and survives restarts.
 */
class DownloadStore(private val ctx: Context, private val prefs: SharedPreferences) {
    private val json = Json { ignoreUnknownKeys = true }
    private val ser = ListSerializer(Dl.serializer())
    private val dm = ctx.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    var items by mutableStateOf(load()); private set

    private fun load(): List<Dl> =
        runCatching { json.decodeFromString(ser, prefs.getString("downloads", null) ?: "[]") }.getOrDefault(emptyList())

    private fun save() = prefs.edit().putString("downloads", json.encodeToString(ser, items)).apply()

    private val dir: File
        get() = (ctx.getExternalFilesDir("downloads") ?: File(ctx.filesDir, "downloads")).also { it.mkdirs() }

    /** Bytes used by saved files, and free space on the same storage. */
    fun usedBytes(): Long = dir.listFiles()?.sumOf { it.length() } ?: 0L
    fun freeBytes(): Long = dir.usableSpace

    fun itemFor(id: String) = items.firstOrNull { it.id == id }
    fun hasActive() = items.any { !it.done && !it.failed }
    fun file(d: Dl) = File(dir, d.file)

    /** Local file addresses for the player (video file, subtitle file). */
    fun playItem(d: Dl) = PlayItem(
        title = d.title,
        url = Uri.fromFile(file(d)).toString(),
        subtitleUrl = d.sub?.let { File(dir, it) }?.takeIf { it.exists() }?.let { Uri.fromFile(it).toString() },
        thumb = d.thumb, key = d.key, duration = d.duration,
    )

    fun enqueue(
        id: String, key: String, lang: String, type: String, title: String, thumb: String?, duration: Double,
        url: String, subUrl: String?,
    ): Boolean {
        itemFor(id)?.let { if (!it.failed) return true else remove(id) }
        if (!url.startsWith("http")) return false
        return try {
            val base = id.replace(Regex("[^A-Za-z0-9_-]"), "_")
            val name = base + if (type == "audio") ".mp3" else ".mp4"
            File(dir, name).delete()
            fun req(u: String, n: String) = DownloadManager.Request(Uri.parse(u))
                .setTitle(title)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN)
                .setDestinationInExternalFilesDir(ctx, "downloads", n)
            val dmId = dm.enqueue(req(url, name))
            var subName: String? = null
            var subId = -1L
            if (!subUrl.isNullOrBlank() && subUrl.startsWith("http")) {
                subName = "$base.vtt"
                File(dir, subName).delete()
                subId = dm.enqueue(req(subUrl, subName))
            }
            items = listOf(Dl(id, key, lang, type, title, thumb, duration, name, subName, dmId, subId)) + items
            save()
            true
        } catch (e: Exception) { false }
    }

    /** Pulls the progress of unfinished downloads from the system. */
    fun refresh() {
        val ids = items.filter { !it.done && !it.failed }.map { it.dmId }.toLongArray()
        if (ids.isEmpty()) return
        val seen = HashMap<Long, Triple<Int, Long, Long>>()
        dm.query(DownloadManager.Query().setFilterById(*ids))?.use { c ->
            while (c.moveToNext()) {
                seen[c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_ID))] = Triple(
                    c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                    c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)),
                    c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                )
            }
        }
        var changed = false
        items = items.map { d ->
            if (d.done || d.failed) d else {
                val t = seen[d.dmId]
                val nd = if (t == null) d.copy(status = DownloadManager.STATUS_FAILED) else d.copy(status = t.first, bytes = t.second, total = t.third)
                if (nd != d) changed = true
                nd
            }
        }
        if (changed) save()
    }

    fun remove(id: String) {
        itemFor(id)?.let {
            runCatching { dm.remove(it.dmId); if (it.subDmId >= 0) dm.remove(it.subDmId) }
            file(it).delete()
            it.sub?.let { s -> File(dir, s).delete() }
        }
        items = items.filter { it.id != id }
        save()
    }
}
