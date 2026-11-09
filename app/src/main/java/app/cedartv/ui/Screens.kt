package app.cedartv.ui

import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Border
import androidx.tv.material3.ButtonDefaults
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.cedartv.R
import app.cedartv.data.Api
import app.cedartv.data.Category
import app.cedartv.data.Media
import app.cedartv.data.toMedia

private sealed interface Load<out T> {
    data object Loading : Load<Nothing>
    data class Fail(val msg: String) : Load<Nothing>
    data class Ok<T>(val v: T) : Load<T>
}

@Composable
private fun Centered(text: String) = Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Text(text, fontSize = 22.sp, color = CedarMuted)
}

private class HomeData(val latest: Category?, val featured: Category?, val top: Category)

@Composable
fun HomeScreen(
    listState: LazyListState,
    lang: String,
    langShort: String,
    busy: Boolean,
    onWatchTv: () -> Unit,
    onMusic: () -> Unit,
    onKaraoke: () -> Unit,
    onSearch: () -> Unit,
    onLibrary: () -> Unit,
    onPlayLib: (List<Media>, Int) -> Unit,
    onSettings: () -> Unit,
    onLanguage: () -> Unit,
    subShort: String?,
    subName: String,
    subBoth: Boolean,
    onSubLang: () -> Unit,
    onSubOff: () -> Unit,
    onSubBoth: (Boolean) -> Unit,
    onOpen: (Category) -> Unit,
    onPlay: (List<Media>, Int) -> Unit,
) {
    var state by remember { mutableStateOf<Load<HomeData>>(Load.Loading) }
    LaunchedEffect(lang) {
        state = Load.Loading
        state = try {
            val latest = runCatching { Api.category(lang, "LatestVideos") }.getOrNull()
            val featured = runCatching { Api.category(lang, "FeaturedLibraryVideos") }.getOrNull()
            val top = Api.category(lang, "VideoOnDemand")
            Load.Ok(HomeData(latest, featured, top))
        } catch (e: Exception) {
            Load.Fail(e.message ?: "Network error")
        }
    }
    val focus = remember { FocusRequester() }
    val cont = LocalLibrary.current.continueList().map { it.toMedia() }
    var subMenu by remember { mutableStateOf(false) }
    BackHandler(enabled = subMenu) { subMenu = false }
    val sid = LocalScreenId.current
    Box(Modifier.fillMaxSize()) {
    LazyColumn(Modifier.fillMaxSize(), state = listState, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        item {
            Column(Modifier.padding(start = 48.dp, end = 48.dp, top = 20.dp)) {
                // Row 1: logo - clock - search, subtitles, language, settings
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(38.dp).background(CedarGreen, CircleShape), contentAlignment = Alignment.Center) { Ico(Icons.Filled.Park, 24.dp, Color.White) }
                        Text("Cedar TV", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Clock()
                        if (Api.servedStale) IconLabel(Icons.Filled.CloudOff, str(R.string.offline_banner), 13.sp, CedarAccent)
                    }
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = onSearch, modifier = Modifier.restoreFocus("top:search")) { Box(Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) { Ico(Icons.Filled.Search, 24.dp) } }
                        Button(onClick = { subMenu = true }, modifier = Modifier.restoreFocus("top:cc")) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                                Ico(Icons.Filled.ClosedCaption, 24.dp, if (subShort != null) CedarAccent else LocalContentColor.current)
                                if (subShort != null) Text(subShort, fontSize = 15.sp, color = CedarAccent)
                            }
                        }
                        Button(onClick = onLanguage, modifier = Modifier.restoreFocus("top:lang")) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) {
                                Ico(Icons.Filled.Language, 24.dp)
                                Text(langShort, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Button(onClick = onSettings, modifier = Modifier.restoreFocus("top:settings")) { Box(Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) { Ico(Icons.Filled.Settings, 24.dp) } }
                    }
                }
                // Row 2: the main things to do; Watch TV stands out
                Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = onWatchTv, modifier = Modifier.focusRequester(focus).onFocusChanged { if (it.hasFocus) FocusMemory.last[sid] = "top:watch" },
                        colors = ButtonDefaults.colors(containerColor = CedarGreen, contentColor = Color.White, focusedContainerColor = Color(0xFF3E9B74), focusedContentColor = Color.White),
                        border = ButtonDefaults.border(
                            border = Border(BorderStroke(2.dp, CedarAccent), shape = RoundedCornerShape(50)),
                            focusedBorder = Border(BorderStroke(3.dp, Color.White), shape = RoundedCornerShape(50)),
                        ),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)) {
                            Ico(Icons.Filled.LiveTv, 26.dp, Color.White)
                            Text(if (busy) str(R.string.tuning_in) else str(R.string.watch_tv), fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    HomeButton(Icons.Filled.VideoLibrary, str(R.string.library), onLibrary, Modifier.restoreFocus("top:library"))
                    HomeButton(Icons.Filled.LibraryMusic, str(R.string.music_audio), onMusic, Modifier.restoreFocus("top:music"))
                    HomeButton(Icons.Filled.Mic, str(R.string.karaoke), onKaraoke, Modifier.restoreFocus("top:karaoke"))
                }
            }
            // Only grab focus on a fresh Home; coming Back keeps the scroll position where it was.
            LaunchedEffect(Unit) {
                if (listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0) runCatching { focus.requestFocus() }
            }
        }
        if (cont.isNotEmpty()) item { MediaRow(str(R.string.continue_watching), cont, onPlayLib) }
        when (val s = state) {
            is Load.Loading -> item { Centered(str(R.string.loading)) }
            is Load.Fail -> item { Centered(str(R.string.could_not_load) + ": " + s.msg) }
            is Load.Ok -> {
                s.v.latest?.takeIf { it.media.isNotEmpty() }?.let { c -> item { MediaRow(c.name, c.media, onPlay) } }
                s.v.featured?.takeIf { it.media.isNotEmpty() }?.let { c -> item { MediaRow(c.name, c.media, onPlay) } }
                item { CategoryRow(s.v.top.name, s.v.top.subcategories, onOpen) }
            }
        }
    }
    if (subMenu) SubtitleMenu(subShort != null, subName, subBoth, onClose = { subMenu = false },
        onLang = { subMenu = false; onSubLang() }, onOff = { subMenu = false; onSubOff() }, onBoth = { subMenu = false; onSubBoth(it) })
    }
}

@Composable
private fun Clock() {
    var now by remember { mutableStateOf(java.util.Date()) }
    LaunchedEffect(Unit) { while (true) { now = java.util.Date(); kotlinx.coroutines.delay(15_000) } }
    Text(remember(now) { java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(now) }, fontSize = 24.sp, fontWeight = FontWeight.Light)
}

/** The "CC" drop-down: subtitles off / language, and whether the original text is shown with the translation. */
@Composable
private fun SubtitleMenu(on: Boolean, name: String, both: Boolean, onClose: () -> Unit, onLang: () -> Unit, onOff: () -> Unit, onBoth: (Boolean) -> Unit) {
    val first = remember { FocusRequester() }
    val langFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(80); runCatching { (if (on) langFocus else first).requestFocus() } }
    Box(Modifier.fillMaxSize().background(Color(0x80000000)), contentAlignment = Alignment.TopEnd) {
        Column(
            Modifier.padding(top = 70.dp, end = 48.dp).width(360.dp).background(Color(0xF2121A16), RoundedCornerShape(14.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            IconLabel(Icons.Filled.ClosedCaption, str(R.string.subtitles), 20.sp, CedarAccent)
            Button(onClick = onOff, modifier = Modifier.fillMaxWidth().focusRequester(first)) {
                IconLabel(if (!on) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked, str(R.string.subs_off))
            }
            Button(onClick = onLang, modifier = Modifier.fillMaxWidth().focusRequester(langFocus)) {
                IconLabel(Icons.Filled.Translate, if (on) name else str(R.string.language) + "…")
            }
            if (on) {
                Button(onClick = { onBoth(true) }, modifier = Modifier.fillMaxWidth()) {
                    IconLabel(if (both) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked, str(R.string.subs_both))
                }
                Button(onClick = { onBoth(false) }, modifier = Modifier.fillMaxWidth()) {
                    IconLabel(if (!both) Icons.Filled.RadioButtonChecked else Icons.Filled.RadioButtonUnchecked, str(R.string.subs_only))
                }
            }
        }
    }
}

@Composable
private fun HomeButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, modifier = modifier) {
        Box(Modifier.padding(horizontal = 6.dp, vertical = 2.dp)) { IconLabel(icon, label, 17.sp) }
    }
}

@Composable
fun CategoryScreen(
    listState: LazyListState,
    lang: String,
    cat: Category,
    onOpen: (Category) -> Unit,
    onPlay: (List<Media>, Int) -> Unit,
) {
    var state by remember { mutableStateOf<Load<Category>>(Load.Loading) }
    LaunchedEffect(cat.key, lang) {
        state = Load.Loading
        state = try { Load.Ok(Api.category(lang, cat.key)) } catch (e: Exception) { Load.Fail(e.message ?: "Network error") }
    }
    Column(Modifier.fillMaxSize()) {
        Text(cat.name, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(start = 48.dp, top = 32.dp))
        when (val s = state) {
            is Load.Loading -> Centered(str(R.string.loading))
            is Load.Fail -> Centered(str(R.string.could_not_load) + ": " + s.msg)
            is Load.Ok -> {
                val c = s.v
                val withMedia = c.subcategories.filter { it.media.isNotEmpty() }
                val tiles = c.subcategories.filter { it.media.isEmpty() }
                LazyColumn(Modifier.fillMaxSize(), state = listState) {
                    if (c.media.isNotEmpty()) item { MediaRow(c.name, c.media, onPlay) }
                    if (tiles.isNotEmpty()) item { CategoryRow("Browse", tiles, onOpen) }
                    withMedia.forEach { sub -> item { MediaRow(sub.name, sub.media, onPlay) } }
                }
            }
        }
    }
}

private class SongRow(val title: String, val songs: List<Media>)

@Composable
fun KaraokeListScreen(lang: String, onPlay: (List<Media>, Int) -> Unit) {
    var state by remember { mutableStateOf<Load<List<SongRow>>>(Load.Loading) }
    LaunchedEffect(lang) {
        state = Load.Loading
        // Only songs whose video has a synced lyrics file. Row titles come from the API (translated).
        val rows = listOf("VODOriginalSongs", "ChildrensSongs", "VODSingToJah").mapNotNull { key ->
            runCatching { Api.category(lang, key) }.getOrNull()?.let { c ->
                SongRow(c.name, c.media.filter { m -> m.files.any { !it.subtitles?.url.isNullOrBlank() } })
            }?.takeIf { it.songs.isNotEmpty() }
        }
        state = if (rows.isEmpty()) Load.Fail("") else Load.Ok(rows)
    }
    Column(Modifier.fillMaxSize()) {
        ScreenTitle(Icons.Filled.Mic, str(R.string.karaoke), Modifier.padding(start = 48.dp, top = 32.dp))
        when (val s = state) {
            is Load.Loading -> Centered(str(R.string.loading))
            is Load.Fail -> Centered(str(R.string.no_lyrics))
            is Load.Ok -> LazyColumn(Modifier.fillMaxSize()) {
                s.v.forEach { row -> item { MediaRow(row.title, row.songs, onPlay, withActions = true) } }
            }
        }
    }
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(lang: String, onPlay: (List<Media>, Int) -> Unit) {
    var query by remember { mutableStateOf("") }
    var index by remember { mutableStateOf<List<Pair<String, Media>>?>(null) }
    var results by remember { mutableStateOf<List<Media>>(emptyList()) }
    var filter by remember { mutableStateOf(0) } // 0 all, 1 videos, 2 songs
    val lib = LocalLibrary.current
    val focus = remember { FocusRequester() }
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    LaunchedEffect(lang) {
        index = null
        index = runCatching { Api.searchIndex(lang) }.getOrDefault(emptyList())
    }
    LaunchedEffect(query, index, filter) {
        val idx = index ?: return@LaunchedEffect
        kotlinx.coroutines.delay(250)
        val q = Api.norm(query.trim())
        results = if (q.isEmpty()) emptyList() else {
            val tokens = q.split(' ').filter { it.isNotEmpty() }
            idx.filter { (t, _) -> tokens.all { t.contains(it) } }
                .sortedBy { (t, _) -> if (t.startsWith(q)) 0 else if (t.contains(q)) 1 else 2 }
                .map { it.second }
                .filter { filter == 0 || (filter == 2) == (it.type == "audio") }.take(60)
        }
    }
    LaunchedEffect(Unit) { kotlinx.coroutines.delay(300); runCatching { focus.requestFocus() }; keyboard?.show() }
    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp)) {
        ScreenTitle(Icons.Filled.Search, str(R.string.search), Modifier.padding(top = 32.dp))
        androidx.compose.foundation.text.BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(color = CedarText, fontSize = 22.sp),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(CedarAccent),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
            keyboardActions = androidx.compose.foundation.text.KeyboardActions(onSearch = { lib.addRecent(query) }),
            modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth().focusRequester(focus),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth().background(CedarCard, androidx.compose.foundation.shape.RoundedCornerShape(10.dp)).padding(16.dp)) {
                    if (query.isEmpty()) Text(str(R.string.search_hint), color = CedarMuted, fontSize = 22.sp)
                    inner()
                }
            },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = { filter = (filter + 1) % 3 }) {
                when (filter) { 1 -> IconLabel(Icons.Filled.Movie, str(R.string.videos)); 2 -> IconLabel(Icons.Filled.MusicNote, str(R.string.songs)); else -> IconLabel(Icons.Filled.Apps, str(R.string.all_types)) }
            }
            if (query.isBlank()) lib.recent.forEach { r -> Button(onClick = { query = r }) { Text(r, fontSize = 18.sp, maxLines = 1) } }
        }
        when {
            index == null -> Centered(str(R.string.loading))
            query.isNotBlank() && results.isEmpty() -> Centered(str(R.string.no_results))
            else -> androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                columns = androidx.compose.foundation.lazy.grid.GridCells.Adaptive(280.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp),
                modifier = Modifier.fillMaxSize(),
            ) {
                items(results, key = { it.naturalKey }) { m ->
                    MediaCard(m) { lib.addRecent(query); onPlay(listOf(m), 0) }
                }
            }
        }
    }
}

@Composable
fun LibraryScreen(onPlay: (List<Media>, Int) -> Unit, onPlayDl: (List<Media>, Int) -> Unit) {
    val lib = LocalLibrary.current
    val dlRow = LocalDownloads.current.items.map { it.toMedia() }
    val cont = lib.continueList().map { it.toMedia() }
    val fav = lib.favorites.map { it.toMedia() }
    val hist = lib.history.map { it.toMedia() }
    Column(Modifier.fillMaxSize()) {
        ScreenTitle(Icons.Filled.VideoLibrary, str(R.string.library), Modifier.padding(start = 48.dp, top = 32.dp))
        IconLabel(Icons.Filled.Info, str(R.string.hold_hint), 15.sp, CedarMuted, modifier = Modifier.padding(start = 48.dp, top = 6.dp))
        if (dlRow.isEmpty() && cont.isEmpty() && fav.isEmpty() && hist.isEmpty()) Centered(str(R.string.empty_library))
        else LazyColumn(Modifier.fillMaxSize()) {
            if (dlRow.isNotEmpty()) item {
                MediaRow(str(R.string.downloads), dlRow, onPlayDl, icon = Icons.Filled.Download)
                Text(storageLine(), fontSize = 14.sp, color = CedarMuted, modifier = Modifier.padding(start = 48.dp))
            }
            if (cont.isNotEmpty()) item { MediaRow(str(R.string.continue_watching), cont, onPlay, icon = Icons.Filled.PlayCircle) }
            if (fav.isNotEmpty()) item { MediaRow(str(R.string.favorites), fav, onPlay, icon = Icons.Filled.Star) }
            if (hist.isNotEmpty()) item {
                MediaRow(str(R.string.history), hist, onPlay, icon = Icons.Filled.History, trailing = {
                    Button(onClick = { lib.clearHistory() }) { IconLabel(Icons.Filled.DeleteSweep, str(R.string.clear_history), 15.sp) }
                })
            }
        }
    }
}

@Composable
private fun storageLine(): String {
    val ctx = LocalUiContext.current
    val dl = LocalDownloads.current
    val used = android.text.format.Formatter.formatShortFileSize(ctx, dl.usedBytes())
    val free = android.text.format.Formatter.formatShortFileSize(ctx, dl.freeBytes())
    return ctx.getString(R.string.storage_line, used, free)
}

private val SS_STEPS = listOf(0, 1, 5, 10, 30)
private val Q_STEPS = listOf(360, 480, 720, 1080)
private val PQ_STEPS = listOf(1080, 720, 480, 360, 240)

@Composable
fun SettingsScreen(
    ssMin: Int, onSs: (Int) -> Unit,
    dlQ: Int, onDlQ: (Int) -> Unit,
    playQ: Int, onPlayQ: (Int) -> Unit,
    onPreview: () -> Unit,
    showNoVideo: Boolean, onNoVideo: (Boolean) -> Unit,
    onTour: () -> Unit,
    onAbout: () -> Unit,
    onReport: () -> Unit,
) {
    val ctx = LocalUiContext.current
    val ssLabel = if (ssMin == 0) str(R.string.subs_off) else ctx.getString(R.string.minutes_short, ssMin)
    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ScreenTitle(Icons.Filled.Settings, str(R.string.settings), Modifier.padding(top = 32.dp, bottom = 8.dp))
        Button(onClick = { onSs(SS_STEPS[(SS_STEPS.indexOf(ssMin) + 1) % SS_STEPS.size]) }) {
            Box(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) { IconLabel(Icons.Filled.Slideshow, str(R.string.screensaver) + ": " + ssLabel, 22.sp) }
        }
        Button(onClick = onPreview) {
            Box(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) { IconLabel(Icons.Filled.PlayArrow, str(R.string.screensaver), 22.sp) }
        }
        Button(onClick = { onPlayQ(PQ_STEPS[(PQ_STEPS.indexOf(playQ) + 1) % PQ_STEPS.size]) }) {
            Box(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) { IconLabel(Icons.Filled.HighQuality, str(R.string.play_quality) + ": " + playQ + "p", 22.sp) }
        }
        Button(onClick = { onDlQ(Q_STEPS[(Q_STEPS.indexOf(dlQ) + 1) % Q_STEPS.size]) }) {
            Box(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) { IconLabel(Icons.Filled.Download, str(R.string.dl_quality) + ": " + dlQ + "p", 22.sp) }
        }
        Text(storageLine(), fontSize = 16.sp, color = CedarMuted, modifier = Modifier.padding(start = 12.dp))
        Button(onClick = { onNoVideo(!showNoVideo) }) {
            Box(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) { IconLabel(Icons.Filled.Public, str(R.string.no_video_langs) + ": " + str(if (showNoVideo) R.string.shown else R.string.hidden), 22.sp) }
        }
        Button(onClick = onTour) {
            Box(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) { IconLabel(Icons.Filled.HelpOutline, str(R.string.how_to), 22.sp) }
        }
        Button(onClick = onReport) {
            Box(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) { IconLabel(Icons.Filled.BugReport, str(R.string.report_problem), 22.sp) }
        }
        Button(onClick = onAbout) {
            Box(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) { IconLabel(Icons.Filled.Info, str(R.string.about), 22.sp) }
        }
    }
}

@Composable
fun AboutScreen(version: String) {
    Row(Modifier.fillMaxSize().padding(horizontal = 64.dp, vertical = 40.dp), horizontalArrangement = Arrangement.spacedBy(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(260.dp)) {
            androidx.compose.foundation.Image(
                androidx.compose.ui.res.painterResource(R.drawable.ic_tree), null,
                Modifier.size(200.dp).clip(RoundedCornerShape(36.dp)).background(Color(0xFF0E1512)),
            )
            Text("Cedar TV", fontSize = 34.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 16.dp))
            Text(if (version.isEmpty()) "" else LocalUiContext.current.getString(R.string.about_version, version), fontSize = 18.sp, color = CedarMuted)
        }
        Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.weight(1f)) {
            Text(str(R.string.about_tagline), fontSize = 28.sp, color = CedarAccent, fontWeight = FontWeight.Medium)
            Text(str(R.string.about_text), fontSize = 21.sp)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.padding(top = 6.dp)) {
                IconLabel(Icons.Filled.Person, str(R.string.about_made_by), 21.sp)
                IconLabel(Icons.Filled.Language, "chapliak.com", 21.sp, tint = CedarAccent)
                IconLabel(Icons.Filled.Code, "github.com/chapliak/cedar-tv", 21.sp, tint = CedarAccent)
            }
            Text(str(R.string.about_legal), fontSize = 16.sp, color = CedarMuted, modifier = Modifier.padding(top = 6.dp))
        }
    }
}


@Composable
fun ReportScreen(version: String, lang: String) {
    val details = remember(version, lang) {
        listOf(
            "Cedar TV $version",
            "Android ${android.os.Build.VERSION.RELEASE} (API ${android.os.Build.VERSION.SDK_INT})",
            "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}",
            "Content language: $lang",
            "Interface: ${java.util.Locale.getDefault().toLanguageTag()}",
        )
    }
    Row(Modifier.fillMaxSize().padding(horizontal = 64.dp, vertical = 40.dp), horizontalArrangement = Arrangement.spacedBy(56.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(360.dp)) {
            androidx.compose.foundation.Image(
                androidx.compose.ui.res.painterResource(R.drawable.qr_issues), null,
                Modifier.size(320.dp).clip(RoundedCornerShape(12.dp)),
            )
            Text(str(R.string.report_scan), fontSize = 18.sp, color = CedarMuted, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.padding(top = 14.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(18.dp), modifier = Modifier.weight(1f)) {
            ScreenTitle(Icons.Filled.BugReport, str(R.string.report_title))
            Text(str(R.string.report_text), fontSize = 21.sp)
            Text(str(R.string.report_details), fontSize = 18.sp, color = CedarAccent, fontWeight = FontWeight.Medium)
            Column(Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFF16201B)).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                details.forEach { Text(it, fontSize = 18.sp) }
            }
            Text(str(R.string.report_alt), fontSize = 16.sp, color = CedarMuted)
        }
    }
}
