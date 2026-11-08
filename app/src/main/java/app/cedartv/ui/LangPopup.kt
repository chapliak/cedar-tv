package app.cedartv.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Translate
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import app.cedartv.R
import app.cedartv.data.Language

/** Last used languages per purpose ("recent_audio", "recent_sub", "recent_lang"), most recent first. */
object RecentLangs {
    const val OFF = "__off"
    fun get(c: Context, key: String): List<String> =
        (c.getSharedPreferences("cedar", Context.MODE_PRIVATE).getString(key, "") ?: "").split(',').filter { it.isNotBlank() }

    fun push(c: Context, key: String, code: String) {
        if (code.isBlank() || code == OFF) return
        val l = (listOf(code) + get(c, key)).distinct().take(5)
        c.getSharedPreferences("cedar", Context.MODE_PRIVATE).edit().putString(key, l.joinToString(",")).apply()
    }

    /** The chosen language first, then the last used ones: at most 5 codes. */
    fun pinned(c: Context, key: String, current: String): List<String> =
        (listOf(current) + get(c, key)).filter { it.isNotBlank() }.distinct().take(5)
}

/** Small list that sits over the video: the chosen language on top, the recent ones below, then "More languages". */
@Composable
fun LangPopup(
    title: String,
    all: List<Language>,
    codes: List<String>,
    current: String,
    withOff: Boolean,
    offLabel: String,
    firstFocus: FocusRequester,
    onFocused: (Boolean) -> Unit,
    onPick: (Language) -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rows = buildList {
        codes.forEach { c -> if (c == RecentLangs.OFF) add(Language(code = RecentLangs.OFF, vernacular = offLabel)) else all.firstOrNull { it.code == c }?.let { add(it) } }
        if (withOff && none { it.code == RecentLangs.OFF }) add(Language(code = RecentLangs.OFF, vernacular = offLabel))
    }
    Column(
        modifier.width(520.dp).background(Color(0xF2121A16), RoundedCornerShape(16.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(title, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        rows.forEachIndexed { i, l ->
            val mod = Modifier.fillMaxWidth().let { if (i == 0) it.focusRequester(firstFocus).onFocusChanged { f -> onFocused(f.isFocused) } else it }
            Button(onClick = { onPick(l) }, modifier = mod) {
                IconLabel(if (l.code == current) Icons.Filled.Check else Icons.Filled.Translate, l.vernacular.ifBlank { l.name })
            }
        }
        val more = Modifier.fillMaxWidth().let { if (rows.isEmpty()) it.focusRequester(firstFocus).onFocusChanged { f -> onFocused(f.isFocused) } else it }
        Button(onClick = onMore, modifier = more) { IconLabel(Icons.Filled.MoreHoriz, str(R.string.more_languages)) }
    }
}
