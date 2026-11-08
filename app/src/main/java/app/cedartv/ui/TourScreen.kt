package app.cedartv.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.cedartv.R
import kotlinx.coroutines.delay

private val TITLES = listOf(R.string.tour_t1, R.string.tour_t2, R.string.tour_t3, R.string.tour_t4, R.string.tour_t5)
private val BODIES = listOf(R.string.tour_b1, R.string.tour_b2, R.string.tour_b3, R.string.tour_b4, R.string.tour_b5)
private val PAGE_ICON = listOf(Icons.Filled.Park, Icons.Filled.SettingsRemote, Icons.Filled.Home, Icons.Filled.ClosedCaption, Icons.Filled.Slideshow)

@Composable
private fun RemoteArt() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(18.dp)) {
        Box(Modifier.size(240.dp).background(CedarCard, CircleShape), contentAlignment = Alignment.Center) {
            Ico(Icons.Filled.KeyboardArrowUp, 40.dp, modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp))
            Ico(Icons.Filled.KeyboardArrowDown, 40.dp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp))
            Ico(Icons.Filled.KeyboardArrowLeft, 40.dp, modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp))
            Ico(Icons.Filled.KeyboardArrowRight, 40.dp, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp))
            Box(Modifier.size(96.dp).background(CedarGreen, CircleShape), contentAlignment = Alignment.Center) {
                Text("OK", fontSize = 30.sp, fontWeight = FontWeight.Bold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.background(CedarCard, RoundedCornerShape(20.dp)).padding(horizontal = 22.dp, vertical = 8.dp)) { Ico(Icons.AutoMirrored.Filled.ArrowBack, 28.dp) }
            Box(Modifier.background(CedarCard, RoundedCornerShape(20.dp)).padding(horizontal = 22.dp, vertical = 8.dp)) { Ico(Icons.Filled.Home, 28.dp) }
        }
    }
}

/** Five short pages: what the app is, the remote, Home, while watching, good to know. */
@Composable
fun TourScreen(onClose: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val last = page == TITLES.lastIndex
    val nextFocus = remember { FocusRequester() }
    LaunchedEffect(page) { delay(100); runCatching { nextFocus.requestFocus() } }

    Row(Modifier.fillMaxSize().padding(horizontal = 64.dp, vertical = 40.dp), horizontalArrangement = Arrangement.spacedBy(48.dp)) {
        Box(Modifier.weight(0.38f).fillMaxHeight(), contentAlignment = Alignment.Center) {
            if (page == 1) RemoteArt()
            else Box(Modifier.size(220.dp).background(CedarCard, CircleShape), contentAlignment = Alignment.Center) {
                Ico(PAGE_ICON[page], 110.dp, CedarAccent)
            }
        }
        Column(Modifier.weight(0.62f).fillMaxHeight(), verticalArrangement = Arrangement.Center) {
            Text(str(TITLES[page]), style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold)
            Text(str(BODIES[page]), fontSize = 24.sp, lineHeight = 36.sp, modifier = Modifier.padding(top = 20.dp, bottom = 28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 22.dp)) {
                TITLES.indices.forEach { i ->
                    Box(Modifier.size(if (i == page) 14.dp else 10.dp).background(if (i == page) CedarAccent else CedarMuted, CircleShape))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                if (page > 0) Button(onClick = { page-- }) { Ico(Icons.AutoMirrored.Filled.ArrowBack, 26.dp, modifier = Modifier.padding(horizontal = 6.dp)) }
                Button(onClick = { if (last) onClose() else page++ }, modifier = Modifier.focusRequester(nextFocus)) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)) {
                        Text(if (last) str(R.string.tour_done) else str(R.string.tour_next), fontSize = 22.sp)
                        if (!last) { Spacer(Modifier.width(8.dp)); Ico(Icons.AutoMirrored.Filled.ArrowForward, 24.dp) }
                    }
                }
                if (!last) Button(onClick = onClose) { Text(str(R.string.tour_skip), fontSize = 20.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
            }
        }
    }
}
