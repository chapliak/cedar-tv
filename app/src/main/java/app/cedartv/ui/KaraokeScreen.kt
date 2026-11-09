package app.cedartv.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import android.view.KeyEvent
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import app.cedartv.R
import app.cedartv.data.Api
import app.cedartv.data.Cue
import app.cedartv.data.PlayItem
import app.cedartv.data.Vtt
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlin.math.ceil

/** Song videos (each with its own synced lyrics file). */
class KaraokeRequest(val items: List<PlayItem>, val start: Int = 0)

/** A lyric cue can have several lines: they are sung one after another, so the highlight runs line by line. */
@Composable
private fun SweepCue(text: String, frac: Float) {
    val lines = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf(text) }
    val total = lines.sumOf { it.length }.coerceAtLeast(1)
    var before = 0
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        lines.forEach { l ->
            val share = l.length.toFloat() / total
            val f = ((frac - before.toFloat() / total) / share).coerceIn(0f, 1f)
            SweepLine(l, f)
            before += l.length
        }
    }
}

@Composable
private fun SweepLine(text: String, frac: Float) {
    Box {
        Text(text, fontSize = 46.sp, lineHeight = 56.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = Color(0xAAFFFFFF))
        Text(
            text, fontSize = 46.sp, lineHeight = 56.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = CedarAccent,
            modifier = Modifier.drawWithContent {
                clipRect(left = 0f, top = 0f, right = size.width * frac, bottom = size.height) {
                    this@drawWithContent.drawContent()
                }
            },
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun KaraokeScreen(req: KaraokeRequest, subs: SubSettings, audioLang: String = "") {
    val ctx = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItems(req.items.map { MediaItem.fromUri(it.url) }, req.start, 0L)
            repeatMode = Player.REPEAT_MODE_ALL
            prepare()
            playWhenReady = true
        }
    }
    var index by remember { mutableIntStateOf(req.start) }
    var playing by remember { mutableStateOf(true) }
    var pos by remember { mutableLongStateOf(0L) }
    var own by remember { mutableStateOf<List<Cue>>(emptyList()) }
    var tr by remember { mutableStateOf<List<Cue>>(emptyList()) }
    var loaded by remember { mutableStateOf(false) }
    var hint by remember { mutableStateOf(true) }
    val focus = remember { FocusRequester() }
    val noLyrics = str(R.string.no_lyrics)

    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) { index = player.currentMediaItemIndex }
            override fun onIsPlayingChanged(isPlaying: Boolean) { playing = isPlaying }
            override fun onPlayerError(error: PlaybackException) {
                if (player.hasNextMediaItem()) { player.seekToNextMediaItem(); player.prepare(); player.play() }
            }
        }
        player.addListener(l)
        onDispose { player.removeListener(l); player.release() }
    }
    LaunchedEffect(index, subs.lang, audioLang) {
        loaded = false; own = emptyList(); tr = emptyList()
        val item = req.items.getOrNull(index) ?: return@LaunchedEffect
        own = item.subtitleUrl?.let { Api.cuesFromUrl(it) } ?: emptyList()
        val lang = subs.lang
        // Same language as the song's own lyrics: a second copy would only repeat the same line.
        if (lang != null && !lang.equals(audioLang, ignoreCase = true) && item.key.isNotBlank()) tr = Api.subtitleTrack(lang, item.key)?.cues ?: emptyList()
        loaded = true
    }
    LaunchedEffect(Unit) { while (isActive) { pos = player.currentPosition; delay(60) } }
    LaunchedEffect(Unit) { delay(300); runCatching { focus.requestFocus() }; delay(6000); hint = false }

    val i = Vtt.indexAt(own, pos)
    val active = i >= 0 && pos < own[i].end + 250
    val cur = if (active) own[i] else null
    val prev = if (active) own.getOrNull(i - 1) else own.getOrNull(i)
    val next = if (active) own.getOrNull(i + 1) else own.getOrNull(i + 1)
    val untilNext = next?.let { it.start - pos }
    val trRaw = if (tr.isNotEmpty()) Vtt.at(tr, pos) else null
    fun norm(t: String) = t.filter { it.isLetterOrDigit() }.lowercase()
    // Also hide it when the text is identical to the lyric on screen (e.g. a regional variant of the same language).
    val trLine = trRaw?.takeIf { cur == null || norm(it) != norm(cur.text) }

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE ->
                        { if (player.isPlaying) player.pause() else player.play(); true }
                    KeyEvent.KEYCODE_DPAD_LEFT -> { player.seekTo((player.currentPosition - 10_000).coerceAtLeast(0)); true }
                    KeyEvent.KEYCODE_DPAD_RIGHT -> { player.seekTo(player.currentPosition + 10_000); true }
                    KeyEvent.KEYCODE_DPAD_UP -> { player.seekToPreviousMediaItem(); true }
                    KeyEvent.KEYCODE_DPAD_DOWN -> { player.seekToNextMediaItem(); true }
                    else -> false
                }
            }
            .focusRequester(focus).focusable(),
    ) {
        // The song video is only a backdrop; the lyrics are the show.
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { c ->
                PlayerView(c).apply {
                    this.player = player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                    keepScreenOn = true
                    isFocusable = false
                }
            },
        )
        Box(Modifier.fillMaxSize().background(Color(0xB3000000)))

        Column(
            Modifier.fillMaxSize().padding(horizontal = 80.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (loaded && own.isEmpty()) {
                Text(noLyrics, fontSize = 28.sp, color = CedarMuted)
            } else {
                Text(prev?.text ?: " ", fontSize = 28.sp, lineHeight = 36.sp, color = Color(0x80FFFFFF), textAlign = TextAlign.Center, maxLines = 2)
                Box(Modifier.padding(vertical = 22.dp), contentAlignment = Alignment.Center) {
                    if (cur != null) {
                        SweepCue(cur.text, ((pos - cur.start).toFloat() / (cur.end - cur.start).coerceAtLeast(1)).coerceIn(0f, 1f))
                    } else if (untilNext != null && untilNext in 0..4000) {
                        Text(ceil(untilNext / 1000f).toInt().coerceAtLeast(1).toString(), fontSize = 72.sp, fontWeight = FontWeight.Bold, color = CedarAccent)
                    } else {
                        Ico(Icons.Filled.MusicNote, 64.dp, Color(0x80FFFFFF))
                    }
                }
                Text(
                    trLine ?: " ", fontSize = 28.sp, lineHeight = 36.sp, color = Color(0xFFFFE27A), fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center, maxLines = 2, modifier = Modifier.padding(bottom = 22.dp),
                )
                Text(next?.takeIf { cur != null }?.text ?: " ", fontSize = 32.sp, lineHeight = 40.sp, color = Color(0x99FFFFFF), textAlign = TextAlign.Center, maxLines = 2)
            }
        }
        Text(
            req.items.getOrNull(index)?.title.orEmpty() + if (req.items.size > 1) "   ${index + 1}/${req.items.size}" else "",
            fontSize = 20.sp, color = Color(0xCCFFFFFF), modifier = Modifier.align(Alignment.TopStart).padding(32.dp),
        )
        if (!playing) Ico(Icons.Filled.Pause, 44.dp, Color.White, Modifier.align(Alignment.TopEnd).padding(32.dp))
        if (hint) Row(
            Modifier.align(Alignment.BottomCenter).padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) { Ico(Icons.Filled.KeyboardArrowLeft, 22.dp, CedarMuted); Ico(Icons.Filled.KeyboardArrowRight, 22.dp, CedarMuted); Text(" 10 s", fontSize = 16.sp, color = CedarMuted) }
            Row(verticalAlignment = Alignment.CenterVertically) { Ico(Icons.Filled.KeyboardArrowUp, 22.dp, CedarMuted); Ico(Icons.Filled.SkipPrevious, 20.dp, CedarMuted); Ico(Icons.Filled.KeyboardArrowDown, 22.dp, CedarMuted); Ico(Icons.Filled.SkipNext, 20.dp, CedarMuted) }
            Row(verticalAlignment = Alignment.CenterVertically) { Text("OK ", fontSize = 16.sp, color = CedarMuted); Ico(Icons.Filled.PlayArrow, 20.dp, CedarMuted); Ico(Icons.Filled.Pause, 20.dp, CedarMuted) }
        }
    }
}
