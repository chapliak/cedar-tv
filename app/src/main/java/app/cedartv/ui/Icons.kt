package app.cedartv.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Icon
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.Text

/** One icon from the Material Icons set (Apache 2.0), tinted like the surrounding text by default. */
@Composable
fun Ico(v: ImageVector, size: Dp = 22.dp, tint: Color = LocalContentColor.current, modifier: Modifier = Modifier) =
    Icon(v, contentDescription = null, tint = tint, modifier = modifier.size(size))

/** Icon followed by a text, for buttons and menu rows. */
@Composable
fun IconLabel(v: ImageVector, text: String, fontSize: TextUnit = 18.sp, tint: Color = LocalContentColor.current, maxLines: Int = 1, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Ico(v, (fontSize.value * 1.25f).dp, tint)
        Text(text, fontSize = fontSize, maxLines = maxLines, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun ScreenTitle(v: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Ico(v, 34.dp, CedarAccent)
        Text(text, style = androidx.tv.material3.MaterialTheme.typography.headlineLarge)
    }
}
