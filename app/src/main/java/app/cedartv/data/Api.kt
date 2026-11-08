package app.cedartv.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap

@Serializable
data class CategoryResp(val category: Category)

@Serializable
data class Category(
    val key: String = "",
    val name: String = "",
    val type: String = "",
    val subcategories: List<Category> = emptyList(),
    val media: List<Media> = emptyList(),
    val images: JsonElement? = null,
)

@Serializable
data class Media(
    val naturalKey: String = "",
    val languageAgnosticNaturalKey: String = "",
    val title: String = "",
    val type: String = "",
    val primaryCategory: String = "",
    val description: String = "",
    val duration: Double = 0.0,
    val durationFormattedHHMM: String = "",
    val tags: List<String> = emptyList(),
    val files: List<MediaFile> = emptyList(),
    val images: JsonElement? = null,
)

@Serializable
data class MediaFile(
    val progressiveDownloadURL: String = "",
    val label: String = "",
    val frameHeight: Int = 0,
    val mimetype: String = "",
    val subtitles: Subtitles? = null,
)

@Serializable
data class Subtitles(val url: String = "")

/** Languages whose "Latest videos" is empty (checked 2026-11-08 with tools/probe_video_languages.py). */
val NO_VIDEO = setOf("IK", "WSL", "DSL", "STD", "GHM", "HMA", "HMS", "DU", "NGP", "NDF", "PAA", "PH", "PCM", "PMR", "RMC", "RM", "SD", "SGS", "VL", "DR", "BM")

@Serializable
data class LanguagesResp(val languages: List<Language> = emptyList())

@Serializable
data class Language(
    val code: String = "",
    val locale: String = "",
    val vernacular: String = "",
    val name: String = "",
    val isRTL: Boolean = false,
    val isSignLanguage: Boolean = false,
) {
    /** Three-letter code for the top bar: ENG, UKR, RUS ... */
    fun shortCode(): String =
        runCatching { java.util.Locale(locale.substringBefore('_').lowercase()).isO3Language.uppercase() }
            .getOrNull()?.takeIf { it.length == 3 } ?: code.take(3).uppercase()

    /** Base language tag for Android resources / Locale, e.g. "cmn_hans" -> "zh", "pt_pt" -> "pt". */
    fun uiTag(): String {
        val base = locale.substringBefore('_').lowercase()
        return when (base) { "cmn" -> "zh"; "iw" -> "he"; else -> base }
    }
}

/** What the player needs; independent of the API shapes. */
data class PlayItem(
    val title: String, val url: String, val subtitleUrl: String?, val thumb: String?,
    val key: String = "", val duration: Double = 0.0,
    /** (height, url) of every file of the video, best first; [height] is the one in [url]. */
    val qualities: List<Pair<Int, String>> = emptyList(), val height: Int = 0,
)

@Serializable
data class MediaItemsResp(val media: List<Media> = emptyList())

/** Subtitles of one video in one language, plus that version's length (to spot different cuts). */
data class SubTrack(val duration: Double, val cues: List<Cue>)

private val IMAGE_RATIOS = listOf("wss", "wsr", "lss", "sqr", "pnr")
// Cards are ~450 px wide on a 1080p TV: "md" is enough and far lighter than "lg"; 4K/8K panels use "lg".
private val SIZES_MD = listOf("md", "lg", "sm", "xs")
private val SIZES_LG = listOf("lg", "md", "sm", "xs")

fun bestImage(images: JsonElement?): String? {
    val obj = images as? JsonObject ?: return null
    for (ratio in IMAGE_RATIOS) {
        val sizes = obj[ratio] as? JsonObject ?: continue
        for (size in if (Api.hiRes) SIZES_LG else SIZES_MD) {
            val p = sizes[size] as? JsonPrimitive ?: continue
            if (p.isString && p.content.isNotBlank()) return p.content
        }
    }
    return null
}

fun Media.thumb(): String? = bestImage(images)

/** Biggest wide picture (screensaver), falling back to the card picture. */
fun Media.artwork(): String? {
    val obj = images as? JsonObject ?: return null
    for ((ratio, sizes) in listOf("lsr" to listOf("xl", "lg", "md"), "wsr" to listOf("lg", "md"), "wss" to listOf("lg", "md", "sm"))) {
        val o = obj[ratio] as? JsonObject ?: continue
        for (sz in sizes) {
            val p = o[sz] as? JsonPrimitive ?: continue
            if (p.isString && p.content.isNotBlank()) return p.content
        }
    }
    return null
}
fun Category.thumb(): String? = bestImage(images)

private fun MediaFile.height(): Int = label.removeSuffix("p").toIntOrNull() ?: frameHeight

fun Media.bestFile(maxHeight: Int = 720): MediaFile? {
    val videos = files.filter { it.mimetype.startsWith("video") && it.progressiveDownloadURL.isNotBlank() }
    if (videos.isEmpty()) return files.firstOrNull { it.progressiveDownloadURL.isNotBlank() }
    return videos.filter { it.height() <= maxHeight }.maxByOrNull { it.height() }
        ?: videos.minByOrNull { it.height() }
}

fun Media.toPlayItem(maxHeight: Int = 720): PlayItem? {
    val f = bestFile(maxHeight) ?: return null
    val qs = files.filter { it.mimetype.startsWith("video") && it.progressiveDownloadURL.isNotBlank() }
        .map { it.height() to it.progressiveDownloadURL }.distinctBy { it.first }.sortedByDescending { it.first }
    return PlayItem(title, f.progressiveDownloadURL, f.subtitles?.url?.takeIf { it.isNotBlank() }, thumb(), languageAgnosticNaturalKey, duration, qs, f.height())
}

private val MUSIC = Regex("song|music|melod|chorus|vocal|instrumental|sing|sjj", RegexOption.IGNORE_CASE)
private fun isMusicKey(k: String) = MUSIC.containsMatchIn(k)
private fun isMusic(m: Media) =
    isMusicKey(m.primaryCategory) || m.tags.any { isMusicKey(it) } || m.type == "audio"

/** Public JW media API (the same data the jw-api gateway wraps). No key needed. */
object Api {
    private const val BASE = "https://b.jw-cdn.org/apis/mediator/v1"
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true; isLenient = true; coerceInputValues = true }
    private val cache = ConcurrentHashMap<String, Category>()

    /** Set at start-up: true on 4K/8K screens (bigger pictures). */
    @Volatile var hiRes = false

    @Volatile private var languagesCache: List<Language>? = null

    private var catalogDir: File? = null
    private const val FRESH_MS = 6 * 3600_000L

    /** True while the last answer came from the saved copy because the network failed. */
    var servedStale by mutableStateOf(false)

    /** Where saved copies of the catalog live (so browsing works offline and starts faster). */
    fun init(cacheDir: File) { catalogDir = File(cacheDir, "catalog").also { it.mkdirs() } }

    private fun diskFile(id: String): File? = catalogDir?.let { File(it, id.replace('/', '_') + ".json") }

    private fun fetchBody(url: String): String {
        val r = client.newCall(Request.Builder().url(url).build()).execute()
        r.use { check(it.isSuccessful) { "HTTP ${it.code}" }; return it.body!!.string() }
    }

    /** All ~312 content languages (sorted by English name). */
    suspend fun languages(): List<Language> {
        languagesCache?.let { return it }
        val f = diskFile("languages")
        val list = withContext(Dispatchers.IO) {
            val fresh = f?.takeIf { it.exists() && System.currentTimeMillis() - it.lastModified() < 7 * 24 * FRESH_MS / 6 }
            val body = fresh?.let { runCatching { it.readText() }.getOrNull() } ?: try {
                fetchBody("$BASE/languages/E/web").also { f?.writeText(it) }
            } catch (e: Exception) {
                f?.takeIf { it.exists() }?.readText() ?: throw e
            }
            json.decodeFromString<LanguagesResp>(body).languages
        }.filter { it.code.isNotBlank() }.sortedBy { it.name.lowercase() }
        languagesCache = list
        return list
    }

    private fun fetchText(url: String): String? =
        client.newCall(Request.Builder().url(url).build()).execute().use { r ->
            if (r.isSuccessful) r.body?.string() else null
        }

    suspend fun cuesFromUrl(url: String): List<Cue> = withContext(Dispatchers.IO) {
        try {
            // Downloaded videos keep their lyrics/subtitles as local files.
            val text = if (url.startsWith("file:")) java.io.File(java.net.URI(url).path).readText() else fetchText(url)
            text?.let { Vtt.parse(it) } ?: emptyList()
        } catch (e: Exception) { emptyList() }
    }

    /** One media item in a language (files, subtitles...), or null. */
    suspend fun mediaItem(lang: String, languageAgnosticKey: String): Media? = withContext(Dispatchers.IO) {
        try {
            fetchText("$BASE/media-items/$lang/$languageAgnosticKey")
                ?.let { json.decodeFromString<MediaItemsResp>(it).media.firstOrNull() }
        } catch (e: Exception) { null }
    }

    /** Same video in another language: its subtitles + duration. Null if that language has none. */
    suspend fun subtitleTrack(lang: String, languageAgnosticKey: String): SubTrack? = withContext(Dispatchers.IO) {
        try {
            val body = fetchText("$BASE/media-items/$lang/$languageAgnosticKey") ?: return@withContext null
            val m = json.decodeFromString<MediaItemsResp>(body).media.firstOrNull() ?: return@withContext null
            val url = m.files.firstNotNullOfOrNull { it.subtitles?.url?.takeIf { u -> u.isNotBlank() } }
                ?: return@withContext null
            val cues = Vtt.parse(fetchText(url) ?: return@withContext null)
            if (cues.isEmpty()) null else SubTrack(m.duration, cues)
        } catch (e: Exception) { null }
    }

    suspend fun category(lang: String, key: String): Category {
        val id = "$lang/$key"
        cache[id]?.let { return it }
        return withContext(Dispatchers.IO) {
            val f = diskFile(id)
            fun decode(text: String) = json.decodeFromString<CategoryResp>(text).category
            // A recent saved copy is as good as the network and much faster.
            f?.takeIf { it.exists() && System.currentTimeMillis() - it.lastModified() < FRESH_MS }
                ?.let { runCatching { decode(it.readText()) }.getOrNull() }
                ?.let { cache[id] = it; return@withContext it }
            try {
                val body = fetchBody("$BASE/categories/$lang/$key?detailed=1")
                val cat = decode(body)
                runCatching { f?.writeText(body) }
                cache[id] = cat
                servedStale = false
                cat
            } catch (e: Exception) {
                // Offline: fall back to an older saved copy rather than an error.
                val old = f?.takeIf { it.exists() }?.let { runCatching { decode(it.readText()) }.getOrNull() }
                if (old != null) { servedStale = true; old } else throw e
            }
        }
    }

    private val indexCache = ConcurrentHashMap<String, List<Pair<String, Media>>>()

    /** Lower-case, accent-free text for matching ("Écoute" -> "ecoute"). */
    fun norm(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "")

    private fun collectAll(c: Category): List<Media> = c.media + c.subcategories.flatMap { collectAll(it) }

    /** Every video and song of the library in one language, with its normalized title for searching. */
    suspend fun searchIndex(lang: String): List<Pair<String, Media>> {
        indexCache[lang]?.let { return it }
        val idx = coroutineScope {
            val keys = listOf("VideoOnDemand", "Audio").flatMap { r ->
                runCatching { category(lang, r).subcategories.map { it.key } }.getOrDefault(emptyList())
            }
            keys.map { k -> async { runCatching { collectAll(category(lang, k)) }.getOrDefault(emptyList()) } }
                .awaitAll().flatten()
        }.filter { it.files.isNotEmpty() && it.title.isNotBlank() }
            .distinctBy { it.naturalKey }
            .map { norm(it.title + " " + it.description) to it }
        indexCache[lang] = idx
        return idx
    }

    /** Landscape artwork for the screensaver: a random selection of videos that have a wide picture. */
    suspend fun artworkPool(lang: String): List<Media> {
        val all = indexCache[lang]?.map { it.second } ?: coroutineScope {
            listOf("LatestVideos", "FeaturedLibraryVideos", "VODStudio", "VODMovies", "VODBible", "VODChildren", "VODFamily", "VODIntExp", "VODOurActivities")
                .map { k -> async { runCatching { collectAll(category(lang, k)) }.getOrDefault(emptyList()) } }
                .awaitAll().flatten()
        }
        return all.filter { it.type == "video" && it.artwork() != null }.distinctBy { it.naturalKey }.shuffled().take(80)
    }

    /** All non-music videos of the on-demand library, for the "Watch TV" button. */
    suspend fun watchTvPool(lang: String): List<Media> = coroutineScope {
        val top = category(lang, "VideoOnDemand")
        val keys = top.subcategories.map { it.key }
            .filterNot { isMusicKey(it) || it == "VODAudioDescriptions" }
        fun collect(c: Category): List<Media> =
            if (isMusicKey(c.key)) emptyList() else c.media + c.subcategories.flatMap { collect(it) }
        keys.map { k -> async { runCatching { collect(category(lang, k)) }.getOrDefault(emptyList()) } }
            .awaitAll().flatten()
            .filter { it.type == "video" && it.duration >= 60 && !isMusic(it) }
            .distinctBy { it.naturalKey }
    }
}
