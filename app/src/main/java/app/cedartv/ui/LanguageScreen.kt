package app.cedartv.ui

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import app.cedartv.R
import app.cedartv.data.Language
import kotlinx.coroutines.delay

@Composable
fun LanguageScreen(title: String, allLangs: List<Language>, current: String, noSign: Boolean = false, pinned: List<String> = emptyList(), onPick: (Language) -> Unit) {
    val langs = remember(allLangs, noSign) { if (noSign) allLangs.filter { !it.isSignLanguage } else allLangs }
    var query by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(0) } // 0 all, 1 sign languages, 2 spoken
    val filtered = remember(langs, query, kind) {
        val q = query.trim()
        val byKind = if (kind == 0) langs else langs.filter { it.code == "__off" || (kind == 1) == it.isSignLanguage }
        if (q.isEmpty() && kind == 0 && pinned.isNotEmpty()) {
            // chosen + last used languages stay pinned on top
            pinned.mapNotNull { c -> byKind.firstOrNull { it.code == c } } + byKind.filter { it.code !in pinned }
        } else if (q.isEmpty()) byKind
        else byKind.filter { it.name.contains(q, true) || it.vernacular.contains(q, true) || it.code.equals(q, true) }
    }
    val grid = rememberLazyGridState()
    val focus = remember { FocusRequester() }
    LaunchedEffect(langs) {
        // Open on the current language so the remote starts there.
        // (pinned languages come first, so normally this is the top of the list; keep two rows above so Up works)
        val idx = filtered.indexOfFirst { it.code == current }
        if (idx >= 0) {
            runCatching { grid.scrollToItem(if (idx < 6) 0 else idx - 6) }
            delay(250)
            runCatching { focus.requestFocus() }
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp)) {
        Text(title, style = MaterialTheme.typography.headlineLarge, modifier = Modifier.padding(top = 32.dp))
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = TextStyle(color = CedarText, fontSize = 20.sp),
            cursorBrush = SolidColor(CedarAccent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(),
            decorationBox = { inner ->
                Box(Modifier.fillMaxWidth().background(CedarCard, RoundedCornerShape(10.dp)).padding(16.dp)) {
                    if (query.isEmpty()) IconLabel(Icons.Filled.Search, str(R.string.search_language), 20.sp, CedarMuted)
                    inner()
                }
            },
        )
        if (!noSign) androidx.tv.material3.Button(onClick = { kind = (kind + 1) % 3 }) {
            when (kind) { 1 -> IconLabel(Icons.Filled.SignLanguage, str(R.string.sign_languages)); 2 -> IconLabel(Icons.Filled.RecordVoiceOver, str(R.string.spoken_languages)); else -> IconLabel(Icons.Filled.Public, str(R.string.all_langs)) }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(300.dp),
            state = grid,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp, top = 8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(filtered, key = { it.code }) { l ->
                val isCurrent = l.code == current
                Card(
                    onClick = { onPick(l) },
                    modifier = Modifier.then(if (isCurrent) Modifier.focusRequester(focus) else Modifier),
                    scale = CardDefaults.scale(focusedScale = 1.05f),
                    border = CardDefaults.border(
                        border = if (isCurrent) Border(BorderStroke(2.dp, CedarGreen), shape = RoundedCornerShape(12.dp)) else Border.None,
                        focusedBorder = Border(BorderStroke(3.dp, CedarAccent), shape = RoundedCornerShape(12.dp)),
                    ),
                ) {
                    androidx.compose.foundation.layout.Row(Modifier.fillMaxWidth().height(76.dp).padding(horizontal = 16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        if (isCurrent) { Ico(Icons.Filled.Check, 22.dp, CedarAccent); androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp)) }
                        Column {
                            Text(l.vernacular.ifBlank { l.name }, fontSize = 20.sp, maxLines = 1)
                            if (l.name.isNotBlank() && l.name != l.vernacular) Text(l.name, fontSize = 14.sp, color = CedarMuted, maxLines = 1)
                        }
                    }
                }
            }
        }
    }
}
