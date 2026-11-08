package app.cedartv.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.focusRequester
import androidx.compose.runtime.remember
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.cedartv.data.Category
import app.cedartv.data.Media
import app.cedartv.data.toStored
import androidx.compose.foundation.background
import app.cedartv.data.thumb
import coil.compose.AsyncImage

@Composable
private fun focusBorder() = CardDefaults.border(
    focusedBorder = Border(BorderStroke(3.dp, CedarAccent), shape = RoundedCornerShape(12.dp)),
)

@Composable
fun Modifier.restoreFocus(key: String): Modifier {
    val sid = LocalScreenId.current
    val req = remember { androidx.compose.ui.focus.FocusRequester() }
    val auto = LocalAutoFocus.current
    val restore = remember {
        (FocusMemory.restoreId == sid && FocusMemory.last[sid] == key) ||
            (auto && FocusMemory.last[sid] == null && FocusMemory.claimed != sid && run { FocusMemory.claimed = sid; true })
    }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (restore) {
            kotlinx.coroutines.delay(150)
            val token = sid + "|" + FocusMemory.last[sid] + "|" + FocusMemory.restoreId
            if (FocusMemory.restoredOnce != token) { FocusMemory.restoredOnce = token; runCatching { req.requestFocus() } }
        }
    }
    return this.focusRequester(req).onFocusChanged { if (it.hasFocus) FocusMemory.last[sid] = key }
}

@Composable
fun MediaCard(m: Media, onClick: () -> Unit) {
    val lib = LocalLibrary.current
    val openMenu = LocalCardMenu.current
    val key = m.languageAgnosticNaturalKey.ifBlank { m.naturalKey }
    Card(
        onClick = onClick,
        onLongClick = { openMenu(m) },
        modifier = Modifier.width(280.dp).restoreFocus(key),
        scale = CardDefaults.scale(focusedScale = 1.05f),
        border = focusBorder(),
    ) {
        Column {
            Box {
                AsyncImage(
                    model = remember(m) { m.thumb() },
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                )
                val frac = lib.fraction(key)
                if (frac > 0f) Box(Modifier.align(Alignment.BottomStart).fillMaxWidth().height(5.dp).background(androidx.compose.ui.graphics.Color(0x99000000))) {
                    Box(Modifier.fillMaxWidth(frac).height(5.dp).background(CedarAccent))
                }
                if (lib.isFav(key)) {
                    Ico(Icons.Filled.Star, 18.dp, CedarAccent,
                        Modifier.align(Alignment.TopEnd).padding(6.dp)
                            .background(androidx.compose.ui.graphics.Color(0xAA000000), RoundedCornerShape(6.dp)).padding(3.dp))
                }
            }
            Column(Modifier.padding(10.dp)) {
                Text(m.title, maxLines = 2, overflow = TextOverflow.Ellipsis, fontSize = 15.sp)
                if (m.durationFormattedHHMM.isNotBlank()) {
                    Text(m.durationFormattedHHMM, fontSize = 13.sp, color = CedarMuted)
                }
            }
        }
    }
}

@Composable
fun CategoryTile(c: Category, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(280.dp).restoreFocus("cat:" + c.key),
        scale = CardDefaults.scale(focusedScale = 1.05f),
        border = focusBorder(),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(12.dp))) {
            AsyncImage(
                model = c.thumb(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
            Text(
                c.name,
                modifier = Modifier.align(Alignment.BottomStart).padding(12.dp),
                fontSize = 18.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun RowTitle(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, trailing: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.padding(start = 48.dp, end = 48.dp, top = 24.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (icon != null) Ico(icon, 26.dp, CedarAccent)
        Text(text, style = MaterialTheme.typography.titleLarge)
        trailing?.invoke()
    }
}

/** Pseudo indexes passed to onPlay by the "Play all" / "Shuffle" cards of song rows. */
const val PLAY_ALL = -1
const val SHUFFLE_ALL = -2

@Composable
private fun ActionCard(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, rowKey: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(190.dp).height(96.dp).restoreFocus("act:" + rowKey + ":" + label),
        scale = CardDefaults.scale(focusedScale = 1.05f),
        border = focusBorder(),
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Ico(icon, 30.dp, CedarAccent)
            androidx.compose.foundation.layout.Spacer(Modifier.width(10.dp))
            Text(label, fontSize = 20.sp, maxLines = 1)
        }
    }
}

@Composable
fun MediaRow(title: String, items: List<Media>, onPlay: (List<Media>, Int) -> Unit, withActions: Boolean = false,
             icon: androidx.compose.ui.graphics.vector.ImageVector? = null, trailing: (@Composable () -> Unit)? = null) {
    RowTitle(title, icon, trailing)
    val songs = items.isNotEmpty() && (withActions || items.all { it.type == "audio" })
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 48.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        if (songs) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    ActionCard(Icons.Filled.PlayArrow, str(app.cedartv.R.string.play_all), title) { onPlay(items, PLAY_ALL) }
                    ActionCard(Icons.Filled.Shuffle, str(app.cedartv.R.string.shuffle), title) { onPlay(items, SHUFFLE_ALL) }
                }
            }
        }
        itemsIndexed(items) { i, m -> MediaCard(m) { onPlay(items, i) } }
    }
}

@Composable
fun CategoryRow(title: String, items: List<Category>, onOpen: (Category) -> Unit) {
    RowTitle(title)
    LazyRow(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 48.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        itemsIndexed(items) { _, c -> CategoryTile(c) { onOpen(c) } }
    }
}
