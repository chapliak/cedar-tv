package app.cedartv.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** Context whose resources follow the chosen content language (not the device language). */
val LocalUiContext = staticCompositionLocalOf<Context> { error("LocalUiContext not provided") }

@Composable
fun str(@StringRes id: Int): String = LocalUiContext.current.getString(id)
