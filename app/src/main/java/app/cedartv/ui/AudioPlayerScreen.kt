package app.cedartv.ui

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import android.view.KeyEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Text
import app.cedartv.R
import app.cedartv.data.PlayItem
import app.cedartv.data.Stored
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale

class AudioRequest(val items: List<PlayItem>, val start: Int = 0, val shuffle: Boolean = false)

private fun fmt(ms: Long): String {
    val s = (ms.coerceAtLeast(0) / 1000).toInt()
    return String.format(Locale.US, "%d:%02d", s / 60, s % 60)
}

private val SLEEP_STEPS = listOf(0, 15, 30, 60)

@Composable
fun AudioPlayerScreen(req: AudioRequest, onKaraoke: (PlayItem) -> Unit) {
    val ctx = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(ctx)
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                true,
            )
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build().apply {
                setMediaItems(
                    req.items.map {
                        MediaItem.Builder().setUri(it.url)
                            .setMediaMetadata(MediaMetadata.Builder().setTitle(it.title).build()).build()
                    },
                    req.start, 0L,
                )
                shuffleModeEnabled = req.shuffle
                repeatMode = Player.REPEAT_MODE_ALL
                prepare()
                playWhenReady = true
            }
    }
    var playing by remember { mutableStateOf(true) }
    var index by remember { mutableIntStateOf(req.start) }
    var pos by remember { mutableLongStateOf(0L) }
    var dur by remember { mutableLongStateOf(0L) }
    var shuffle by remember { mutableStateOf(req.shuffle) }
    var repeat by remember { mutableIntStateOf(Player.REPEAT_MODE_ALL) }
    var sleepMin by remember { mutableIntStateOf(0) }
    val playFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()

    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onEvents(p: Player, events: Player.Events) {
                playing = p.isPlaying
                index = p.currentMediaItemIndex
                shuffle = p.shuffleModeEnabled
                repeat = p.repeatMode
                dur = p.duration.takeIf { it != C.TIME_UNSET } ?: 0L
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                // A broken track must not stop the music.
                if (player.hasNextMediaItem()) { player.seekToNextMediaItem(); player.prepare(); player.play() }
            }
        }
        player.addListener(l)
        onDispose { player.removeListener(l); player.release() }
    }
    LaunchedEffect(Unit) {
        while (isActive) { pos = player.currentPosition; delay(500) }
    }
    LaunchedEffect(index) { runCatching { listState.animateScrollToItem((index - 2).coerceAtLeast(0)) } }
    LaunchedEffect(sleepMin) {
        if (sleepMin > 0) { delay(sleepMin * 60_000L); player.pause(); sleepMin = 0 }
    }
    LaunchedEffect(Unit) { delay(300); runCatching { playFocus.requestFocus() } }

    val item = req.items.getOrNull(index)
    val seekStep = 10_000L
    val lib = LocalLibrary.current
    val dls = LocalDownloads.current
    val contentLang = LocalContentLang.current
    val toastFn = LocalToast.current
    val uiCtx = LocalUiContext.current
    fun stored(t: PlayItem?) = t?.takeIf { it.key.isNotBlank() }?.let { Stored(it.key, "audio", it.title, it.thumb, it.duration) }
    LaunchedEffect(index) { stored(item)?.let { lib.addHistory(it) } }

    Row(
        Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 36.dp)
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.nativeKeyEvent.keyCode) {
                    KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> { if (player.isPlaying) player.pause() else player.play(); true }
                    KeyEvent.KEYCODE_MEDIA_PLAY -> { player.play(); true }
                    KeyEvent.KEYCODE_MEDIA_PAUSE -> { player.pause(); true }
                    KeyEvent.KEYCODE_MEDIA_NEXT -> { player.seekToNextMediaItem(); true }
                    KeyEvent.KEYCODE_MEDIA_PREVIOUS -> { player.seekToPrevious(); true }
                    KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> { player.seekTo(player.currentPosition + seekStep); true }
                    KeyEvent.KEYCODE_MEDIA_REWIND -> { player.seekTo((player.currentPosition - seekStep).coerceAtLeast(0)); true }
                    else -> false
                }
            },
        horizontalArrangement = Arrangement.spacedBy(40.dp),
    ) {
        // Now playing
        Column(Modifier.weight(0.46f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            AsyncImage(
                model = item?.thumb, contentDescription = null, contentScale = ContentScale.Crop,
                modifier = Modifier.size(260.dp).clip(RoundedCornerShape(16.dp)).background(CedarCard),
            )
            Text(
                item?.title ?: "", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 20.dp),
            )
            Row(Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(fmt(pos), fontSize = 14.sp, color = CedarMuted)
                Box(Modifier.weight(1f).padding(horizontal = 12.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color0x33)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(if (dur > 0) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f).background(CedarAccent))
                }
                Text(fmt(dur), fontSize = 14.sp, color = CedarMuted)
            }
            Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { player.seekToPrevious() }) { Ico(Icons.Filled.SkipPrevious, 26.dp) }
                Button(onClick = { player.seekTo((player.currentPosition - seekStep).coerceAtLeast(0)) }) { Ico(Icons.Filled.FastRewind, 26.dp) }
                Button(onClick = { if (player.isPlaying) player.pause() else player.play() }, modifier = Modifier.focusRequester(playFocus)) {
                    Ico(if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow, 30.dp)
                }
                Button(onClick = { player.seekTo(player.currentPosition + seekStep) }) { Ico(Icons.Filled.FastForward, 26.dp) }
                Button(onClick = { player.seekToNextMediaItem() }) { Ico(Icons.Filled.SkipNext, 26.dp) }
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = { player.shuffleModeEnabled = !player.shuffleModeEnabled }) {
                    Ico(Icons.Filled.Shuffle, 24.dp, if (shuffle) CedarAccent else CedarMuted)
                }
                Button(onClick = {
                    player.repeatMode = when (player.repeatMode) {
                        Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
                        Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
                        else -> Player.REPEAT_MODE_ALL
                    }
                }) {
                    when (repeat) {
                        Player.REPEAT_MODE_ONE -> Ico(Icons.Filled.RepeatOne, 24.dp, CedarAccent)
                        Player.REPEAT_MODE_ALL -> Ico(Icons.Filled.Repeat, 24.dp, CedarAccent)
                        else -> Ico(Icons.Filled.Repeat, 24.dp, CedarMuted)
                    }
                }
                Button(onClick = { sleepMin = SLEEP_STEPS[(SLEEP_STEPS.indexOf(sleepMin) + 1) % SLEEP_STEPS.size] }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Ico(Icons.Filled.Bedtime, 24.dp, if (sleepMin == 0) CedarMuted else CedarAccent)
                        if (sleepMin != 0) Text("$sleepMin min", fontSize = 16.sp, color = CedarAccent)
                    }
                }
            }
            Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(onClick = {
                    stored(item)?.let {
                        val now = lib.toggleFav(it)
                        toastFn(uiCtx.getString(if (now) R.string.fav_added else R.string.fav_removed))
                    }
                }) { val fav = item != null && lib.isFav(item.key); Ico(if (fav) Icons.Filled.Star else Icons.Filled.StarBorder, 24.dp, if (fav) CedarAccent else CedarMuted) }
                item?.takeIf { it.key.isNotBlank() }?.let { t ->
                    val dlId = "$contentLang|${t.key}"
                    val d = dls.itemFor(dlId)
                    if (t.url.startsWith("http") || d != null) Button(onClick = {
                        if (d == null || d.failed) {
                            val ok = dls.enqueue(dlId, t.key, contentLang, "audio", t.title, t.thumb, t.duration, t.url, null)
                            toastFn(uiCtx.getString(if (ok) R.string.dl_started else R.string.could_not_load))
                        } else {
                            dls.remove(dlId)
                            toastFn(uiCtx.getString(R.string.dl_removed))
                        }
                    }) {
                        when {
                            d == null -> Ico(Icons.Filled.Download, 24.dp)
                            d.done -> Ico(Icons.Filled.DownloadDone, 24.dp, CedarAccent)
                            d.failed -> Ico(Icons.Filled.ErrorOutline, 24.dp)
                            else -> Text("${d.pct}%", fontSize = 16.sp)
                        }
                    }
                }
                Button(onClick = { req.items.getOrNull(index)?.let(onKaraoke) }) { Ico(Icons.Filled.Mic, 24.dp) }
            }
        }
        // Queue
        Column(Modifier.weight(0.54f).fillMaxHeight()) {
            Text(str(R.string.queue), fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 10.dp))
            LazyColumn(state = listState, verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxSize()) {
                itemsIndexed(req.items) { i, t ->
                    Card(
                        onClick = { player.seekTo(i, 0L); player.play() },
                        modifier = Modifier.fillMaxWidth(),
                        scale = CardDefaults.scale(focusedScale = 1.02f),
                    ) {
                        Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (i == index) { Ico(Icons.Filled.PlayArrow, 20.dp, CedarAccent); Spacer(Modifier.width(8.dp)) }
                            Text(
                                t.title, fontSize = 17.sp, maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (i == index) CedarAccent else CedarText,
                                fontWeight = if (i == index) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f),
                            )
                            if (t.duration > 0) Text(fmt((t.duration * 1000).toLong()), fontSize = 14.sp, color = CedarMuted)
                        }
                    }
                }
            }
        }
    }
}

private val Color0x33 = androidx.compose.ui.graphics.Color(0x33FFFFFF)
