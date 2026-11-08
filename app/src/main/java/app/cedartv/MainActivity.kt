package app.cedartv

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.cedartv.data.Api
import app.cedartv.data.NO_VIDEO
import app.cedartv.data.Category
import app.cedartv.data.Language
import app.cedartv.data.Daily
import app.cedartv.data.DownloadStore
import app.cedartv.data.Media
import app.cedartv.data.bestFile
import app.cedartv.data.thumb
import app.cedartv.data.toMedia
import app.cedartv.data.toStored
import app.cedartv.ui.AboutScreen
import app.cedartv.ui.TourScreen
import app.cedartv.ui.CardMenu
import app.cedartv.ui.LocalCardMenu
import app.cedartv.ui.LocalScreenId
import app.cedartv.ui.FocusMemory
import app.cedartv.ui.LocalAutoFocus
import app.cedartv.ui.LocalContentLang
import app.cedartv.ui.LocalDownloads
import app.cedartv.ui.Screensaver
import app.cedartv.ui.SettingsScreen
import kotlinx.coroutines.isActive
import app.cedartv.data.toPlayItem
import app.cedartv.ui.RecentLangs
import app.cedartv.ui.CategoryScreen
import app.cedartv.ui.CedarBg
import app.cedartv.ui.CedarCard
import app.cedartv.ui.CedarTheme
import app.cedartv.ui.HomeScreen
import app.cedartv.ui.LanguageScreen
import app.cedartv.ui.LocalUiContext
import app.cedartv.ui.PlayRequest
import app.cedartv.ui.AudioPlayerScreen
import app.cedartv.data.LibraryStore
import app.cedartv.ui.LibraryScreen
import app.cedartv.ui.LocalLibrary
import app.cedartv.ui.LocalToast
import app.cedartv.ui.SearchScreen
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import app.cedartv.ui.KaraokeListScreen
import app.cedartv.ui.KaraokeRequest
import app.cedartv.ui.KaraokeScreen
import app.cedartv.ui.AudioRequest
import app.cedartv.ui.PLAY_ALL
import app.cedartv.ui.SHUFFLE_ALL
import app.cedartv.ui.PlayerScreen
import app.cedartv.ui.SubSettings
import app.cedartv.ui.str
import kotlinx.coroutines.launch
import java.util.Locale

private sealed interface Screen {
    data object Home : Screen
    data object Languages : Screen
    data object SubLanguages : Screen
    data class Audio(val req: AudioRequest) : Screen
    data object KaraokeList : Screen
    data object Search : Screen
    data object Library : Screen
    data object Settings : Screen
    data object About : Screen
    data object Tour : Screen
    data class Karaoke(val req: KaraokeRequest) : Screen
    data class Cat(val cat: Category) : Screen
    data class Play(val req: PlayRequest) : Screen
}

/** Idle tracking for the screensaver: any key resets the timer; a key while it shows only closes it. */
object Idle {
    @Volatile var last = SystemClock.elapsedRealtime()
    @Volatile var shownAt = 0L
    var active by mutableStateOf(false)

    fun show() { shownAt = SystemClock.elapsedRealtime(); active = true }
}

/** The UI is laid out as if the screen were this many dp wide, on every resolution (720p, 1080p, 4K, 8K). */
const val DESIGN_WIDTH_DP = 1200f

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("cedar", MODE_PRIVATE)
        Api.init(cacheDir)
        // The app has its own screensaver: keep Google's standby/ambient screen from covering the app.
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        // Lighter images: RGB_565 (half the memory/upload), no cross-fade, bigger memory cache.
        coil.Coil.setImageLoader(coil.ImageLoader.Builder(this).allowRgb565(true).crossfade(false)
            .memoryCache { coil.memory.MemoryCache.Builder(this).maxSizePercent(0.25).build() }.build())
        val lib = LibraryStore(prefs)
        val dl = DownloadStore(applicationContext, prefs)
        setContent {
            // The layout was drawn for ~1280 dp wide; Google TV at 1080p reports 960 dp, so everything looked too big.
            val d = androidx.compose.ui.platform.LocalDensity.current
            val dm = resources.displayMetrics
            val widthPx = maxOf(dm.widthPixels, dm.heightPixels).toFloat()
            Api.hiRes = widthPx > 2200f          // 4K/8K panels get the bigger card pictures
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(widthPx / DESIGN_WIDTH_DP, d.fontScale),
            ) { CedarTheme { App(prefs, lib, dl) } }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val now = SystemClock.elapsedRealtime()
        Idle.last = now
        if (Idle.active) {
            // Ignore the tail of the key press that started it (e.g. the OK that clicked "preview").
            if (now - Idle.shownAt > 700) Idle.active = false
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}

private fun deviceTag(): String {
    val t = Locale.getDefault().language.lowercase()
    return if (t == "iw") "he" else t
}

@Composable
private fun App(prefs: SharedPreferences, lib: LibraryStore, dl: DownloadStore) {
    val baseCtx = LocalContext.current
    var lang by remember { mutableStateOf(prefs.getString("lang", null) ?: "E") }
    var subLang by remember { mutableStateOf(prefs.getString("sub", "") ?: "") }
    var subBoth by remember { mutableStateOf(prefs.getBoolean("subBoth", true)) }
    var langs by remember { mutableStateOf<List<Language>>(emptyList()) }
    // First start: the tour opens over Home; Back or Skip closes it. It is marked as seen when it is shown.
    var showNoVideo by remember { mutableStateOf(prefs.getBoolean("show_novideo", false)) }
    var stack by remember { mutableStateOf<List<Screen>>(if (prefs.getBoolean("tour_done", false)) listOf(Screen.Home) else listOf(Screen.Home, Screen.Tour)) }
    var busy by remember { mutableStateOf(false) }
    var toast by remember { mutableStateOf<String?>(null) }
    var ssMin by remember { mutableIntStateOf(prefs.getInt("ss_min", 5)) }
    var dlQ by remember { mutableIntStateOf(prefs.getInt("dl_q", 480)) }
    var playQ by remember { mutableIntStateOf(prefs.getInt("play_q", 720)) }
    val homeList = rememberLazyListState()
    val catLists = remember { HashMap<String, LazyListState>() }
    var menu by remember { mutableStateOf<Media?>(null) }
    var ssPool by remember { mutableStateOf<List<Media>>(emptyList()) }
    var verse by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun chooseLang(code: String, remember: Boolean = true) {
        lang = code
        if (remember) prefs.edit().putString("lang", code).apply()
    }

    // Load all languages; on the very first start pick the one matching the device language.
    LaunchedEffect(Unit) {
        langs = runCatching { Api.languages() }.getOrDefault(emptyList())
        if (prefs.getString("lang", null) == null && langs.isNotEmpty()) {
            val dev = deviceTag()
            langs.firstOrNull { !it.isSignLanguage && it.uiTag() == dev }?.let { chooseLang(it.code) }
        }
    }

    val cur = langs.firstOrNull { it.code == lang }
    // App texts follow the content language; fall back to English where there is no translation.
    val uiCtx = remember(cur?.uiTag()) {
        val tag = cur?.uiTag()?.takeIf { it.isNotBlank() } ?: "en"
        val cfg = Configuration(baseCtx.resources.configuration)
        cfg.setLocale(Locale.forLanguageTag(tag))
        baseCtx.createConfigurationContext(cfg)
    }
    val dir = if (cur?.isRTL == true) LayoutDirection.Rtl else LayoutDirection.Ltr

    LaunchedEffect(toast) { if (toast != null) { delay(3500); toast = null } }
    LaunchedEffect(stack.last()) { if (stack.last() is Screen.Tour) prefs.edit().putBoolean("tour_done", true).apply() }

    BackHandler(enabled = stack.size > 1) { stack = stack.dropLast(1) }
    BackHandler(enabled = menu != null) { menu = null }

    // Downloads: keep the progress current while something is downloading.
    LaunchedEffect(Unit) {
        dl.refresh()
        while (isActive) { if (dl.hasActive()) dl.refresh(); delay(2000) }
    }
    // Screensaver after the chosen idle time (not over a playing video).
    LaunchedEffect(ssMin) {
        while (isActive) {
            delay(5_000)
            val top = stack.last()
            if (ssMin > 0 && !Idle.active && top !is Screen.Play && top !is Screen.Karaoke &&
                SystemClock.elapsedRealtime() - Idle.last > ssMin * 60_000L) Idle.show()
        }
    }
    LaunchedEffect(Idle.active) {
        if (Idle.active) {
            verse = runCatching { Daily.get(prefs, lang) }.getOrNull() ?: verse
            ssPool = runCatching { Api.artworkPool(lang) }.getOrDefault(emptyList())
                .ifEmpty { (lib.favorites + lib.history).map { it.toMedia() } }
        }
    }

    fun play(items: List<Media>, index: Int) {
        val list = items.mapNotNull { it.toPlayItem(playQ) }
        if (list.isEmpty()) { toast = uiCtx.getString(R.string.no_playable); return }
        val key = items.getOrNull(index)?.toPlayItem(playQ)?.url
        val start = list.indexOfFirst { it.url == key }.coerceAtLeast(0)
        if (items.all { it.type == "audio" }) {
            // Songs: music player; "Shuffle" starts on a random track.
            val shuffle = index == SHUFFLE_ALL
            stack = stack + Screen.Audio(AudioRequest(list, if (shuffle) list.indices.random() else start, shuffle))
        } else {
            val resume = items.getOrNull(index)?.let { lib.resumeMs(it.languageAgnosticNaturalKey.ifBlank { it.naturalKey }) } ?: 0L
            stack = stack + Screen.Play(PlayRequest(list, start, startPosMs = resume))
        }
    }

    // Library cards carry no files: look the real item up again (in the current language), then play it.
    fun playLib(row: List<Media>, index: Int) {
        scope.launch {
            try {
                val clicked = row.getOrNull(index)
                if (clicked?.type == "audio" || (index < 0 && row.all { it.type == "audio" })) {
                    val resolved = coroutineScope {
                        row.filter { it.type == "audio" }.take(80)
                            .map { r -> async { Api.mediaItem(lang, r.languageAgnosticNaturalKey) } }.awaitAll()
                    }.filterNotNull()
                    if (resolved.isEmpty()) { toast = uiCtx.getString(R.string.no_playable); return@launch }
                    val at = if (clicked != null) resolved.indexOfFirst { it.languageAgnosticNaturalKey == clicked.languageAgnosticNaturalKey }.coerceAtLeast(0) else index
                    play(resolved, at)
                } else {
                    val m = clicked?.let { Api.mediaItem(lang, it.languageAgnosticNaturalKey) }
                    if (m == null) toast = uiCtx.getString(R.string.no_playable) else play(listOf(m), 0)
                }
            } catch (e: Exception) {
                toast = uiCtx.getString(R.string.could_not_load) + ": " + (e.message ?: "")
            }
        }
    }

    fun startDownload(m: Media) {
        scope.launch {
            val key = m.languageAgnosticNaturalKey.ifBlank { m.naturalKey }
            val full = if (m.files.isNotEmpty()) m else Api.mediaItem(lang, key)
            val f = full?.bestFile(dlQ)
            if (full == null || f == null) { toast = uiCtx.getString(R.string.no_playable); return@launch }
            val ok = dl.enqueue("$lang|$key", key, lang, full.type.ifBlank { "video" }, full.title, full.thumb(), full.duration,
                f.progressiveDownloadURL, f.subtitles?.url)
            toast = uiCtx.getString(if (ok) R.string.dl_started else R.string.could_not_load)
        }
    }

    // Play from the Downloads row: local files, no network needed.
    fun playDownload(row: List<Media>, index: Int) {
        val ready = row.mapNotNull { dl.itemFor(it.naturalKey) }.filter { it.done }
        val clicked = row.getOrNull(index)?.let { dl.itemFor(it.naturalKey) }
        val allAudio = row.isNotEmpty() && row.all { it.type == "audio" }
        if (clicked != null && clicked.done && clicked.type != "audio") {
            val resume = lib.resumeMs(clicked.key)
            stack = stack + Screen.Play(PlayRequest(listOf(dl.playItem(clicked)), 0, startPosMs = resume))
        } else if ((clicked != null && clicked.done && clicked.type == "audio") || (index < 0 && allAudio)) {
            val songs = ready.filter { it.type == "audio" }
            if (songs.isEmpty()) return
            val items = songs.map { dl.playItem(it) }
            val shuffle = index == SHUFFLE_ALL
            val start = if (shuffle) items.indices.random() else songs.indexOfFirst { it.id == clicked?.id }.coerceAtLeast(0)
            stack = stack + Screen.Audio(AudioRequest(items, start, shuffle))
        } else {
            toast = uiCtx.getString(R.string.downloading)
        }
    }

    // Karaoke = the song's video (its lyrics are timed to it, the MP3 is cut differently).
    fun playMenuItem(m: Media) = when {
        '|' in m.naturalKey -> playDownload(listOf(m), 0)
        m.files.isNotEmpty() -> play(listOf(m), 0)
        else -> playLib(listOf(m), 0)
    }

    fun karaokeFrom(songs: List<Media>, index: Int) {
        val list = songs.mapNotNull { it.toPlayItem(480) }.filter { it.subtitleUrl != null }
        if (list.isEmpty()) { toast = uiCtx.getString(R.string.no_lyrics); return }
        val shuffle = index == SHUFFLE_ALL
        val key = songs.getOrNull(index)?.toPlayItem(480)?.url
        val start = if (shuffle) list.indices.random() else list.indexOfFirst { it.url == key }.coerceAtLeast(0)
        val q = if (shuffle) list.shuffled() else list
        stack = stack + Screen.Karaoke(KaraokeRequest(q, if (shuffle) 0 else start))
    }

    fun karaokeForTrack(track: app.cedartv.data.PlayItem) {
        scope.launch {
            val item = Api.mediaItem(lang, track.key.replace("_AUDIO", "_VIDEO"))?.toPlayItem(480)
            if (item?.subtitleUrl == null) toast = uiCtx.getString(R.string.no_lyrics)
            else { toast = null; stack = stack + Screen.Karaoke(KaraokeRequest(listOf(item), 0)) }
        }
    }

    fun watchTv() {
        if (busy) return
        busy = true
        toast = null
        scope.launch {
            try {
                val items = Api.watchTvPool(lang).shuffled().mapNotNull { it.toPlayItem(playQ) }
                if (items.isEmpty()) toast = uiCtx.getString(R.string.no_videos)
                else stack = stack + Screen.Play(PlayRequest(items, 0, tvMode = true))
            } catch (e: Exception) {
                toast = uiCtx.getString(R.string.could_not_load) + ": " + (e.message ?: "")
            } finally {
                busy = false
            }
        }
    }

    val offLabel = uiCtx.getString(R.string.subs_off)
    val subName = langs.firstOrNull { it.code == subLang }?.vernacular ?: offLabel

    // Back to a screen: its cards restore the focus they had (see FocusMemory).
    val screenId = "${stack.size}:${stack.last()::class.simpleName}"
    val prevSize = remember { intArrayOf(stack.size) }
    if (stack.size < prevSize[0]) FocusMemory.restoreId = screenId
    else if (stack.size > prevSize[0]) { FocusMemory.last.remove(screenId); if (FocusMemory.claimed == screenId) FocusMemory.claimed = null }
    prevSize[0] = stack.size

    CompositionLocalProvider(
        LocalUiContext provides uiCtx, LocalLayoutDirection provides dir,
        LocalLibrary provides lib, LocalToast provides { m: String -> toast = m },
        LocalDownloads provides dl, LocalContentLang provides lang, LocalCardMenu provides { m: Media -> menu = m },
        LocalScreenId provides screenId,
        LocalAutoFocus provides (stack.last() is Screen.Cat || stack.last() is Screen.KaraokeList || stack.last() is Screen.Library),
    ) {
        Box(Modifier.fillMaxSize().background(CedarBg)) {
            when (val s = stack.last()) {
                is Screen.Home -> HomeScreen(
                    homeList, lang, cur?.shortCode() ?: "ENG", busy, ::watchTv,
                    onMusic = { stack = stack + Screen.Cat(Category(key = "Audio", name = uiCtx.getString(R.string.music_audio))) },
                    onKaraoke = { stack = stack + Screen.KaraokeList },
                    onSearch = { stack = stack + Screen.Search },
                    onLibrary = { stack = stack + Screen.Library },
                    onPlayLib = ::playLib,
                    onSettings = { stack = stack + Screen.Settings },
                    onLanguage = { stack = stack + Screen.Languages },
                    subShort = if (subLang.isEmpty()) null else langs.firstOrNull { it.code == subLang }?.shortCode(),
                    subName = subName,
                    subBoth = subBoth,
                    onSubLang = { stack = stack + Screen.SubLanguages },
                    onSubOff = { subLang = ""; prefs.edit().putString("sub", "").apply() },
                    onSubBoth = { subBoth = it; prefs.edit().putBoolean("subBoth", it).apply() },
                    onOpen = { stack = stack + Screen.Cat(it) },
                    onPlay = ::play,
                )
                is Screen.SubLanguages -> LanguageScreen(
                    uiCtx.getString(R.string.subtitles),
                    listOf(Language(code = "__off", vernacular = offLabel)) + langs,
                    subLang.ifEmpty { "__off" },
                    noSign = true,
                    pinned = RecentLangs.pinned(uiCtx, "recent_sub", subLang.ifEmpty { "__off" }),
                ) {
                    RecentLangs.push(uiCtx, "recent_sub", it.code)
                    subLang = if (it.code == "__off") "" else it.code
                    prefs.edit().putString("sub", subLang).apply()
                    stack = stack.dropLast(1)
                }
                is Screen.Languages -> LanguageScreen(uiCtx.getString(R.string.language), if (showNoVideo) langs else langs.filter { it.code !in NO_VIDEO || it.code == lang }, lang, pinned = RecentLangs.pinned(uiCtx, "recent_lang", lang)) {
                    RecentLangs.push(uiCtx, "recent_lang", it.code)
                    chooseLang(it.code)
                    toast = null
                    stack = listOf(Screen.Home)
                }
                is Screen.Cat -> CategoryScreen(
                    catLists.getOrPut(s.cat.key) { LazyListState() }, lang, s.cat,
                    onOpen = { stack = stack + Screen.Cat(it) },
                    onPlay = ::play,
                )
                is Screen.Audio -> AudioPlayerScreen(s.req, ::karaokeForTrack)
                is Screen.Search -> SearchScreen(lang, ::play)
                is Screen.Library -> LibraryScreen(::playLib, ::playDownload)
                is Screen.Settings -> SettingsScreen(
                    ssMin, { ssMin = it; prefs.edit().putInt("ss_min", it).apply() },
                    dlQ, { dlQ = it; prefs.edit().putInt("dl_q", it).apply() },
                    playQ, { playQ = it; prefs.edit().putInt("play_q", it).apply() },
                    onPreview = { Idle.show() },
                    showNoVideo = showNoVideo, onNoVideo = { showNoVideo = it; prefs.edit().putBoolean("show_novideo", it).apply() },
                    onTour = { stack = stack + Screen.Tour },
                    onAbout = { stack = stack + Screen.About },
                )
                is Screen.Tour -> TourScreen(onClose = { stack = stack.dropLast(1) })
                is Screen.About -> AboutScreen(
                    runCatching { baseCtx.packageManager.getPackageInfo(baseCtx.packageName, 0).versionName }.getOrNull() ?: "",
                )
                is Screen.KaraokeList -> KaraokeListScreen(lang, ::karaokeFrom)
                is Screen.Karaoke -> KaraokeScreen(s.req, SubSettings(subLang.ifEmpty { null }, subBoth))
                is Screen.Play -> PlayerScreen(s.req, SubSettings(subLang.ifEmpty { null }, subBoth), langs) { l, b ->
                    subLang = l ?: ""
                    subBoth = b
                    prefs.edit().putString("sub", subLang).putBoolean("subBoth", b).apply()
                }
            }
            menu?.let { m ->
                val key = m.languageAgnosticNaturalKey.ifBlank { m.naturalKey }
                val id = if ('|' in m.naturalKey) m.naturalKey else "$lang|$key"
                val d = dl.itemFor(id)
                CardMenu(
                    title = m.title, isFav = lib.isFav(key), dl = d,
                    canStartOver = lib.resumeMs(key) > 0 && m.type != "audio",
                    onStartOver = { lib.forget(key); menu = null; playMenuItem(m) },
                    onPlay = { menu = null; playMenuItem(m) },
                    onFav = {
                        val now = lib.toggleFav(m.toStored())
                        toast = uiCtx.getString(if (now) R.string.fav_added else R.string.fav_removed)
                    },
                    onDownload = {
                        if (d == null || d.failed) startDownload(m)
                        else { dl.remove(id); toast = uiCtx.getString(R.string.dl_removed) }
                    },
                    onClose = { menu = null },
                )
            }
            toast?.let {
                Text(
                    it, fontSize = 18.sp,
                    modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)
                        .background(CedarCard, RoundedCornerShape(8.dp)).padding(16.dp),
                )
            }
            if (Idle.active) Screensaver(ssPool, verse, Locale.forLanguageTag(cur?.uiTag()?.takeIf { it.isNotBlank() } ?: "en"))
        }
    }
}
