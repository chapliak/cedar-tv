package app.cedartv.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import app.cedartv.R
import app.cedartv.data.Media
import app.cedartv.data.artwork
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.DateFormat
import java.util.Date
import java.util.Locale

/** Time one picture stays on screen; the zoom lasts the same time, so it is slow and calm. */
private const val SLIDE_MS = 30_000

@Composable
private fun KenBurns(url: String?) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(url) { scale.animateTo(1.12f, tween(SLIDE_MS + 2_000, easing = LinearEasing)) }
    AsyncImage(
        model = url, contentDescription = null, contentScale = ContentScale.Crop,
        modifier = Modifier.fillMaxSize().graphicsLayer { scaleX = scale.value; scaleY = scale.value },
    )
}

/** Idle-time slideshow: artwork of different videos with titles, clock, date and the daily text. */
@Composable
fun Screensaver(pool: List<Media>, verse: String?, locale: Locale) {
    var i by remember { mutableIntStateOf(0) }
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(pool) { while (isActive) { delay(SLIDE_MS.toLong()); if (pool.size > 1) i = (i + 1) % pool.size } }
    LaunchedEffect(Unit) { while (isActive) { now = Date(); delay(10_000) } }
    val time = remember(now, locale) { DateFormat.getTimeInstance(DateFormat.SHORT, locale).format(now) }
    val date = remember(now, locale) { DateFormat.getDateInstance(DateFormat.FULL, locale).format(now) }
    val m = pool.getOrNull(i)

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        Crossfade(targetState = m, animationSpec = tween(1600), label = "art") { mm ->
            if (mm != null) KenBurns(mm.artwork())
        }
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to Color(0x99000000), 0.25f to Color.Transparent,
                    0.5f to Color.Transparent, 1f to Color(0xE6000000),
                ),
            ),
        )
        Column(Modifier.align(Alignment.TopEnd).padding(48.dp), horizontalAlignment = Alignment.End) {
            Text(time, fontSize = 72.sp, fontWeight = FontWeight.Light, color = Color.White)
            Text(date, fontSize = 22.sp, color = Color(0xCCFFFFFF))
        }
        Column(Modifier.align(Alignment.BottomStart).fillMaxWidth(0.82f).padding(horizontal = 64.dp, vertical = 48.dp)) {
            if (verse != null) {
                IconLabel(Icons.Filled.MenuBook, str(R.string.daily_text), 17.sp, CedarAccent)
                Text(verse, fontSize = 26.sp, fontStyle = FontStyle.Italic, color = Color.White, maxLines = 4, lineHeight = 36.sp, modifier = Modifier.padding(top = 4.dp, bottom = 22.dp))
            }
            if (m != null) Text(m.title, fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 2, lineHeight = 42.sp)
        }
    }
}
