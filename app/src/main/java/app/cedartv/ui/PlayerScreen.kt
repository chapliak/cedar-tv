package app.cedartv.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import android.net.Uri
import android.view.KeyEvent
import android.view.View
import androidx.activity.compose.BackHandler
import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import app.cedartv.R
import app.cedartv.data.Api
import androidx.compose.ui.draw.alpha
import app.cedartv.data.toPlayItem
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.RecordVoiceOver
import app.cedartv.data.Cue
import app.cedartv.data.Language
import androidx.media3.common.C
import app.cedartv.data.PlayItem
import app.cedartv.data.Stored
import app.cedartv.data.Vtt
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.util.Locale
import kotlin.math.abs

/** Learner subtitles: [lang] = MEPS code of the subtitle language (null = off); [both] also shows the video's own language. */
data class SubSettings(val lang: String?, val both: Boolean)

class PlayRequest(val items: List<PlayItem>, val start: Int = 0, val tvMode: Boolean = false, val startPosMs: Long = 0L)

private const val OFF = "__off"

private fun PlayItem.toMediaItem(): MediaItem {
    val b = MediaItem.Builder()
        .setUri(url)
        .setMediaMetadata(MediaMetadata.Builder().setTitle(title).build())
    if (subtitleUrl != null) {
        // Offered in the player's own CC menu, off by default.
        b.setSubtitleConfigurations(
            listOf(
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(subtitleUrl))
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLabel("Subtitles")
                    .setSelectionFlags(0)
                    .build(),
            ),
        )
    }
    return b.build()
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    req: PlayRequest,
    subs: SubSettings,
    langs: List<Language>,
    onSubs: (lang: String?, both: Boolean) -> Unit,
) {
    val ctx = LocalContext.current
    val player = remember {
        ExoPlayer.Builder(ctx).build().apply {
            setMediaItems(req.items.map { it.toMediaItem() }, req.start, req.startPosMs)
            repeatMode = if (req.tvMode) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            prepare()
            playWhenReady = true
        }
    }
    var title by remember { mutableStateOf(req.items.getOrNull(req.start)?.title ?: "") }
    var showTitle by remember { mutableStateOf(true) }
    var index by remember { mutableIntStateOf(req.start) }
    var trCues by remember { mutableStateOf<List<Cue>>(emptyList()) }
    var ownCues by remember { mutableStateOf<List<Cue>>(emptyList()) }
    var line by remember { mutableStateOf<Pair<String?, String?>>(null to null) }
    var notice by remember { mutableStateOf<String?>(null) }
    // Timing of the translation for the current video, ms; > 0 = translation shown later.
    var offsetMs by remember(index) { mutableIntStateOf(0) }
    var panelOpen by remember { mutableStateOf(false) }
    var qOverride by remember(index) { mutableStateOf<Int?>(null) }
    var pickLang by remember { mutableStateOf(false) }
    var settingsMode by remember { mutableStateOf(false) }
    var pickAudio by remember { mutableStateOf(false) }
    var popSub by remember { mutableStateOf(false) }
    var popAudio by remember { mutableStateOf(false) }
    var speed by remember { mutableStateOf(1f) }
    var overrides by remember { mutableStateOf(mapOf<Int, PlayItem>()) }
    var audioLang by remember { mutableStateOf("") }
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var controllerVisible by remember { mutableStateOf(false) }
    var playerView by remember { mutableStateOf<PlayerView?>(null) }
    val panelFocus = remember { FocusRequester() }
    var panelFocused by remember { mutableStateOf(false) }
    val missingMsg = str(R.string.sub_missing)
    val syncMsg = str(R.string.sub_sync)
    val offLabel = str(R.string.subs_off)
    val lib = LocalLibrary.current
    val dls = LocalDownloads.current
    val contentLang = LocalContentLang.current
    val audioNA = str(R.string.audio_unavailable)
    fun curItem(i: Int): PlayItem? = overrides[i] ?: req.items.getOrNull(i)
    val toastFn = LocalToast.current
    val uiCtx = LocalUiContext.current
    fun stored(i: Int): Stored? =
        req.items.getOrNull(i)?.takeIf { it.key.isNotBlank() }?.let { Stored(it.key, "video", it.title, it.thumb, it.duration) }
    // Remember where we stopped (not for the random Watch TV channel).
    fun saveNow() {
        if (req.tvMode) return
        val s = stored(player.currentMediaItemIndex) ?: return
        val d = player.duration
        if (d != C.TIME_UNSET && d > 120_000) lib.saveProgress(s, player.currentPosition, d)
    }

    fun closePanel() {
        panelOpen = false
        pickLang = false
        pickAudio = false
        popSub = false
        popAudio = false
        settingsMode = false
        playerView?.requestFocus()
    }

    fun doAudio(l: Language) {
        val it0 = curItem(index)
        closePanel()
        RecentLangs.push(ctx, "recent_audio", l.code)
        if (it0 != null && it0.key.isNotBlank()) scope.launch {
            val pi = Api.mediaItem(l.code, it0.key)?.toPlayItem(it0.height.takeIf { h -> h > 0 } ?: 720)
            if (pi == null) { notice = audioNA; delay(3500); notice = null }
            else {
                val pos = player.currentPosition
                overrides = overrides + (index to pi)
                player.replaceMediaItem(index, pi.toMediaItem())
                player.seekTo(index, pos)
                audioLang = l.code; qOverride = null
            }
        }
    }
    fun doSub(l: Language) {
        RecentLangs.push(ctx, "recent_sub", l.code)
        onSubs(if (l.code == OFF) null else l.code, subs.both)
        closePanel()
    }

    // Remote = Google TV Streamer (D-pad + Back only). (no ▲ shortcut any more)
    // the CC button in the control bar does the same; Menu/CC/Info keys are a bonus on other remotes.
    val keyHandler by rememberUpdatedState<(KeyEvent) -> Boolean> { e ->
        if (e.action != KeyEvent.ACTION_DOWN) false
        else when {
            e.keyCode == KeyEvent.KEYCODE_MENU || e.keyCode == KeyEvent.KEYCODE_CAPTIONS ||
                e.keyCode == KeyEvent.KEYCODE_INFO || e.keyCode == KeyEvent.KEYCODE_SETTINGS -> { panelOpen = true; true }
            // Jellyfin-style scrubbing: tap: back 5 s / forward 10 s; holding speeds up (x2, x4, x9).
            (e.keyCode == KeyEvent.KEYCODE_DPAD_LEFT || e.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) && !panelOpen &&
                playerView?.let { pv -> !pv.isControllerFullyVisible || pv.findFocus()?.id == androidx.media3.ui.R.id.exo_progress } == true -> {
                val n = e.repeatCount
                val fwd = when { n < 4 -> 10_000L; n < 10 -> 20_000L; n < 20 -> 40_000L; else -> 90_000L }
                val step = if (e.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) fwd / 2 else fwd
                val dir = if (e.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) -1 else 1
                val dur = if (player.duration > 0) player.duration else Long.MAX_VALUE
                player.seekTo((player.currentPosition + dir * step).coerceIn(0L, dur))
                playerView?.let { pv ->
                    pv.showController()
                    // keep the focus on the time bar so the next presses keep scrubbing instead of moving over the buttons
                    pv.findViewById<View>(androidx.media3.ui.R.id.exo_progress)?.requestFocus()
                }
                true
            }
            else -> false
        }
    }

    DisposableEffect(player) {
        val l = object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                title = mediaItem?.mediaMetadata?.title?.toString() ?: ""
                index = player.currentMediaItemIndex
                showTitle = true
                playerView?.post { if (!panelOpen) playerView?.requestFocus() }
            }

            override fun onPlayerError(error: PlaybackException) {
                // In TV mode a broken video must not stop the channel: skip to the next one.
                if (req.tvMode && player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                    player.prepare()
                    player.play()
                }
            }
        }
        player.addListener(l)
        onDispose { saveNow(); player.removeListener(l); player.release() }
    }
    LaunchedEffect(title, showTitle) {
        if (showTitle) { delay(5000); showTitle = false }
    }
    // The Android PlayerView holds the key focus; hand it over to the Compose panel (retry until it really took it).
    LaunchedEffect(panelOpen, pickLang, pickAudio, settingsMode, popSub, popAudio) {
        val pv = playerView
        if (panelOpen) {
            pv?.hideController()
            pv?.descendantFocusability = android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS
            pv?.isFocusable = false   // releases the key focus to the Compose root
            if (!pickLang && !pickAudio) {
                panelFocused = false
                var tries = 0
                while (!panelFocused && tries < 25) { runCatching { panelFocus.requestFocus() }; delay(120); tries++ }
            }
        } else {
            panelFocused = false
            pv?.descendantFocusability = android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS
            pv?.isFocusable = true
            pv?.requestFocus()
        }
    }
    BackHandler(enabled = controllerVisible && !panelOpen) { playerView?.hideController() }
    BackHandler(enabled = panelOpen) { if (pickLang || pickAudio || popSub || popAudio) { pickLang = false; pickAudio = false; popSub = false; popAudio = false } else closePanel() }
    LaunchedEffect(index) { if (!req.tvMode) stored(index)?.let { lib.addHistory(it) } }
    LaunchedEffect(index) { while (isActive) { delay(10_000); saveNow() } }

    // Load the second-language subtitles for the current video.
    LaunchedEffect(index, subs.lang, subs.both, overrides[index]?.url) {
        trCues = emptyList(); ownCues = emptyList(); notice = null
        val lang = subs.lang ?: return@LaunchedEffect
        val item = curItem(index) ?: return@LaunchedEffect
        if (item.key.isBlank()) return@LaunchedEffect
        val tr = Api.subtitleTrack(lang, item.key)
        if (tr == null) { notice = missingMsg; delay(4000); notice = null; return@LaunchedEffect }
        trCues = tr.cues
        if (subs.both && item.subtitleUrl != null) ownCues = Api.cuesFromUrl(item.subtitleUrl)
        // A different cut of the video (other length) means the timing can drift.
        if (item.duration > 0 && abs(tr.duration - item.duration) > 2.5) { notice = syncMsg; delay(6000); notice = null }
    }
    // Pick the cue for the current playback position.
    LaunchedEffect(trCues, ownCues, offsetMs) {
        while (isActive) {
            val t = player.currentPosition
            line = (if (ownCues.isNotEmpty()) Vtt.at(ownCues, t) else null) to
                (if (trCues.isNotEmpty()) Vtt.at(trCues, t - offsetMs) else null)
            delay(100)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { c ->
                object : PlayerView(c) {
                    override fun dispatchKeyEvent(event: KeyEvent): Boolean =
                        keyHandler(event) || super.dispatchKeyEvent(event)
                }.apply {
                    this.player = player
                    useController = true
                    controllerShowTimeoutMs = 4000
                    keepScreenOn = true
                    isFocusable = true
                    setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { v ->
                        controllerVisible = v == View.VISIBLE
                        // a hidden control must not leave the keys without a focus owner (D-pad dead after auto-advance)
                        if (v != View.VISIBLE && !panelOpen) post { requestFocus() }
                    })
                    // CC button in the visible bottom bar (left of the player's gear), reachable with the D-pad.
                    findViewById<android.view.ViewGroup>(androidx.media3.ui.R.id.exo_basic_controls)?.let { bar ->
                        val d = c.resources.displayMetrics.density
                        val focusBg = android.graphics.drawable.GradientDrawable().apply { cornerRadius = 12 * d; setColor(0x66FFFFFF) }
                        val bg = android.graphics.drawable.StateListDrawable().apply {
                            addState(intArrayOf(android.R.attr.state_focused), focusBg)
                            addState(intArrayOf(), android.graphics.drawable.ColorDrawable(0))
                        }
                        bar.addView(android.widget.ImageButton(c).apply {
                            setImageResource(app.cedartv.R.drawable.ic_cc)
                            contentDescription = "Subtitles"
                            isFocusable = true; isClickable = true; background = bg
                            scaleType = android.widget.ImageView.ScaleType.CENTER_INSIDE
                            setPadding((10 * d).toInt(), (6 * d).toInt(), (10 * d).toInt(), (6 * d).toInt())
                            setOnClickListener { panelOpen = true }
                        }, 0, android.view.ViewGroup.LayoutParams((52 * d).toInt(), (44 * d).toInt()))
                    }
                    // our own settings panel instead of the player's popup
                    findViewById<View>(androidx.media3.ui.R.id.exo_settings)?.setOnClickListener { settingsMode = true; panelOpen = true }
                    playerView = this
                    post { requestFocus() }
                }
            },
        )
        Column(
            Modifier.align(Alignment.BottomCenter).alpha(if (panelOpen) 0f else 1f).padding(start = 64.dp, end = 64.dp, bottom = if (controllerVisible) 150.dp else 84.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            line.first?.let {
                Text(it, fontSize = 26.sp, color = Color.White, textAlign = TextAlign.Center,
                    modifier = Modifier.background(Color(0xB3000000)).padding(horizontal = 10.dp, vertical = 2.dp))
            }
            line.second?.let {
                Text(it, fontSize = 28.sp, color = Color(0xFFFFE27A), fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp).background(Color(0xB3000000)).padding(horizontal = 10.dp, vertical = 2.dp))
            }
        }
        notice?.let {
            Text(it, fontSize = 18.sp, color = Color.White,
                modifier = Modifier.align(Alignment.TopCenter).padding(32.dp).background(Color(0xCC000000)).padding(12.dp))
        }
        AnimatedVisibility(visible = showTitle || controllerVisible, modifier = Modifier.align(Alignment.TopStart)) {
            Text(
                (if (req.tvMode) "Cedar TV  ·  " else "") + title,
                fontSize = 22.sp,
                color = Color.White,
                modifier = Modifier.padding(32.dp).background(Color(0x99000000)).padding(12.dp),
            )
        }

        if (panelOpen && !pickLang && !pickAudio && !settingsMode && !popSub) {
            val subName = langs.firstOrNull { it.code == subs.lang }?.vernacular ?: offLabel
            Column(
                Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 96.dp).width(520.dp)
                    .background(Color(0xF2121A16), RoundedCornerShape(16.dp)).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(str(R.string.subtitles), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Button(onClick = { popSub = true }, modifier = Modifier.fillMaxWidth().focusRequester(panelFocus).onFocusChanged { panelFocused = it.isFocused }) {
                    IconLabel(Icons.Filled.ClosedCaption, subName)
                }
                if (subs.lang != null) {
                    Button(onClick = { onSubs(subs.lang, !subs.both) }, modifier = Modifier.fillMaxWidth()) {
                        IconLabel(Icons.Filled.SwapHoriz, str(if (subs.both) R.string.subs_both else R.string.subs_only))
                    }
                    Text(
                        str(R.string.sub_timing) + ":  " + str(R.string.sub_earlier) + "  ‹  " +
                            String.format(Locale.US, "%+.1f s", offsetMs / 1000f) + "  ›  " + str(R.string.sub_later),
                        fontSize = 16.sp, color = CedarMuted,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("−0.5" to -500, "−0.1" to -100, "0" to 0, "+0.1" to 100, "+0.5" to 500).forEach { (label, d) ->
                            Button(
                                onClick = { offsetMs = if (d == 0) 0 else (offsetMs + d).coerceIn(-30_000, 30_000) },
                                modifier = Modifier.weight(1f),
                            ) { Text(label, fontSize = 16.sp) }
                        }
                    }
                }
                Button(onClick = { closePanel() }, modifier = Modifier.fillMaxWidth()) {
                    IconLabel(Icons.Filled.Close, str(R.string.close))
                }
            }
        }
        if (panelOpen && settingsMode && !pickAudio && !popAudio) {
            Column(
                Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 96.dp).width(520.dp)
                    .background(Color(0xF2121A16), RoundedCornerShape(16.dp)).padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(str(R.string.video_settings), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Button(onClick = {
                    val steps = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)
                    speed = steps[(steps.indexOf(speed) + 1) % steps.size]
                    player.setPlaybackSpeed(speed)
                }, modifier = Modifier.fillMaxWidth().focusRequester(panelFocus).onFocusChanged { panelFocused = it.isFocused }) {
                    IconLabel(Icons.Filled.Speed, str(R.string.playback_speed) + ": " + (if (speed == 1f) "1" else speed.toString().trimEnd('0').trimEnd('.')) + "×")
                }
                if (curItem(index)?.key?.isNotBlank() == true) {
                    val aName = langs.firstOrNull { it.code == (audioLang.ifBlank { contentLang }) }?.vernacular ?: contentLang
                    Button(onClick = { popAudio = true }, modifier = Modifier.fillMaxWidth()) {
                        IconLabel(Icons.Filled.RecordVoiceOver, str(R.string.audio_language) + ": " + aName)
                    }
                }
                curItem(index)?.takeIf { it.qualities.size > 1 }?.let { it3 ->
                    val curH = qOverride ?: it3.height
                    Button(onClick = {
                        val hs = it3.qualities.map { q -> q.first }
                        val next = hs[(hs.indexOf(curH) + 1) % hs.size]
                        val url = it3.qualities.first { q -> q.first == next }.second
                        val pos = player.currentPosition
                        player.replaceMediaItem(index, it3.copy(url = url).toMediaItem())
                        player.seekTo(index, pos)
                        qOverride = next
                    }, modifier = Modifier.fillMaxWidth()) {
                        IconLabel(Icons.Filled.HighQuality, str(R.string.play_quality) + ": " + curH + "p")
                    }
                }
                stored(index)?.let { cur ->
                    Button(onClick = {
                        val now = lib.toggleFav(cur)
                        toastFn(uiCtx.getString(if (now) R.string.fav_added else R.string.fav_removed))
                    }, modifier = Modifier.fillMaxWidth()) {
                        IconLabel(if (lib.isFav(cur.key)) Icons.Filled.Star else Icons.Filled.StarBorder, str(R.string.favorite))
                    }
                }
                curItem(index)?.takeIf { it.key.isNotBlank() && it.url.startsWith("http") || dls.itemFor("$contentLang|${it.key}") != null }?.let { it2 ->
                    val dlId = "$contentLang|${it2.key}"
                    val d = dls.itemFor(dlId)
                    Button(onClick = {
                        if (d == null || d.failed) {
                            val ok = dls.enqueue(dlId, it2.key, contentLang, "video", it2.title, it2.thumb, it2.duration, it2.url, it2.subtitleUrl)
                            toastFn(uiCtx.getString(if (ok) R.string.dl_started else R.string.could_not_load))
                        } else {
                            dls.remove(dlId)
                            toastFn(uiCtx.getString(R.string.dl_removed))
                        }
                    }, modifier = Modifier.fillMaxWidth()) {
                        when {
                            d == null -> IconLabel(Icons.Filled.Download, str(R.string.download))
                            d.done -> IconLabel(Icons.Filled.Delete, str(R.string.remove_download))
                            d.failed -> IconLabel(Icons.Filled.ErrorOutline, str(R.string.download))
                            else -> IconLabel(Icons.Filled.Close, str(R.string.downloading) + " " + d.pct + "%")
                        }
                    }
                }
                Button(onClick = { closePanel() }, modifier = Modifier.fillMaxWidth()) {
                    IconLabel(Icons.Filled.Close, str(R.string.close))
                }
            }
        }
        if (panelOpen && popAudio) {
            val cur = audioLang.ifBlank { contentLang }
            LangPopup(
                str(R.string.audio_language), langs, RecentLangs.pinned(ctx, "recent_audio", cur), cur, false, offLabel,
                panelFocus, { panelFocused = it }, { doAudio(it) }, { popAudio = false; pickAudio = true },
                Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 96.dp),
            )
        }
        if (panelOpen && popSub) {
            val cur = subs.lang ?: OFF
            LangPopup(
                str(R.string.subtitles), langs, RecentLangs.pinned(ctx, "recent_sub", cur), cur, true, offLabel,
                panelFocus, { panelFocused = it }, { doSub(it) }, { popSub = false; pickLang = true },
                Modifier.align(Alignment.BottomEnd).padding(end = 24.dp, bottom = 96.dp),
            )
        }
        if (panelOpen && pickAudio) {
            Box(Modifier.fillMaxSize().background(CedarBg)) {
                LanguageScreen(
                    str(R.string.audio_language),
                    langs.filter { it.code !in app.cedartv.data.NO_VIDEO },
                    audioLang.ifBlank { contentLang },
                    noSign = true,
                    pinned = RecentLangs.pinned(ctx, "recent_audio", audioLang.ifBlank { contentLang }),
                ) { l -> doAudio(l) }
            }
        }
        if (panelOpen && pickLang) {
            Box(Modifier.fillMaxSize().background(CedarBg)) {
                LanguageScreen(
                    str(R.string.subtitles),
                    listOf(Language(code = OFF, vernacular = offLabel)) + langs,
                    subs.lang ?: OFF,
                    noSign = true,
                    pinned = RecentLangs.pinned(ctx, "recent_sub", subs.lang ?: OFF),
                ) { doSub(it) }
            }
        }
    }
}
