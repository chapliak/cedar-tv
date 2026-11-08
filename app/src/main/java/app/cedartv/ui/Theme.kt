package app.cedartv.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

val CedarBg = Color(0xFF0E1512)
val CedarCard = Color(0xFF16201B)
val CedarGreen = Color(0xFF2E7D5B)
val CedarAccent = Color(0xFFE8B84A)
val CedarText = Color(0xFFEAF2EE)
val CedarMuted = Color(0xFF9DB1A7)

@Composable
fun CedarTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = CedarGreen,
            onPrimary = Color.White,
            background = CedarBg,
            onBackground = CedarText,
            surface = CedarCard,
            onSurface = CedarText,
            surfaceVariant = CedarCard,
            onSurfaceVariant = CedarMuted,
        ),
        typography = androidx.tv.material3.Typography().let {
            // The default headline styles carry a dark colour of their own; use the light text colour.
            // Fixed line heights of the default styles squeeze or stretch lines when a Text overrides fontSize:
            // use the font's natural line height everywhere.
            fun androidx.compose.ui.text.TextStyle.nat() = copy(lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified)
            it.copy(
                displayLarge = it.displayLarge.nat(), displayMedium = it.displayMedium.nat(),
                headlineMedium = it.headlineMedium.nat(), headlineSmall = it.headlineSmall.nat(),
                titleMedium = it.titleMedium.nat(), titleSmall = it.titleSmall.nat(),
                bodyLarge = it.bodyLarge.nat(), bodyMedium = it.bodyMedium.nat(), bodySmall = it.bodySmall.nat(),
                labelLarge = it.labelLarge.nat(), labelMedium = it.labelMedium.nat(), labelSmall = it.labelSmall.nat(),
                displaySmall = it.displaySmall.copy(color = CedarText).nat(),
                headlineLarge = it.headlineLarge.copy(color = CedarText).nat(),
                titleLarge = it.titleLarge.copy(color = CedarText).nat(),
            )
        },
        content = {
            // Plain screens are not inside a Surface, so set the default text colour once for the whole app.
            androidx.compose.runtime.CompositionLocalProvider(androidx.tv.material3.LocalContentColor provides CedarText, content = content)
        },
    )
}
