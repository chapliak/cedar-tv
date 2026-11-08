package app.cedartv.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import app.cedartv.R
import app.cedartv.data.Dl
import kotlinx.coroutines.delay

/** Menu opened by holding OK on a card. Back closes it. */
@Composable
fun CardMenu(
    title: String,
    isFav: Boolean,
    dl: Dl?,
    canStartOver: Boolean,
    onStartOver: () -> Unit,
    onPlay: () -> Unit,
    onFav: () -> Unit,
    onDownload: () -> Unit,
    onClose: () -> Unit,
) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { delay(100); runCatching { first.requestFocus() } }
    Box(Modifier.fillMaxSize().background(Color(0xCC000000)), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(520.dp).background(Color(0xF2121A16), RoundedCornerShape(16.dp)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            Button(onClick = onPlay, modifier = Modifier.fillMaxWidth().focusRequester(first)) {
                IconLabel(Icons.Filled.PlayArrow, str(R.string.play))
            }
            if (canStartOver) Button(onClick = onStartOver, modifier = Modifier.fillMaxWidth()) {
                IconLabel(Icons.Filled.Replay, str(R.string.start_over))
            }
            Button(onClick = onFav, modifier = Modifier.fillMaxWidth()) {
                IconLabel(if (isFav) Icons.Filled.Star else Icons.Filled.StarBorder, str(R.string.favorite))
            }
            Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) {
                when {
                    dl == null -> IconLabel(Icons.Filled.Download, str(R.string.download))
                    dl.done -> IconLabel(Icons.Filled.Delete, str(R.string.remove_download))
                    dl.failed -> IconLabel(Icons.Filled.ErrorOutline, str(R.string.download))
                    else -> IconLabel(Icons.Filled.Close, str(R.string.downloading) + " " + dl.pct + "%")
                }
            }
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) { IconLabel(Icons.Filled.Close, str(R.string.close)) }
        }
    }
}
