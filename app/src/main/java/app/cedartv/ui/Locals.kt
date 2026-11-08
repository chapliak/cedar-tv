package app.cedartv.ui

import androidx.compose.runtime.staticCompositionLocalOf
import app.cedartv.data.LibraryStore

val LocalLibrary = staticCompositionLocalOf<LibraryStore> { error("LibraryStore not provided") }

/** Shows a short message at the bottom of the screen. */
val LocalToast = staticCompositionLocalOf<(String) -> Unit> { {} }

val LocalDownloads = staticCompositionLocalOf<app.cedartv.data.DownloadStore> { error("DownloadStore not provided") }

/** Content language (MEPS code) the app currently uses. */
val LocalContentLang = staticCompositionLocalOf { "E" }

/** Opens the long-press menu (play, favorite, download) for a card. */
val LocalCardMenu = staticCompositionLocalOf<(app.cedartv.data.Media) -> Unit> { {} }

/** Id of the screen being drawn (stack depth + type); focus memory is kept per screen. */
val LocalScreenId = staticCompositionLocalOf { "" }

/** Remembers which card had focus on each screen, so Back returns to it. */
val LocalAutoFocus = staticCompositionLocalOf { false }

object FocusMemory {
    var claimed: String? = null
    val last = mutableMapOf<String, String>()
    /** Screen id that was just revealed by Back (cards of that screen restore focus), else null. */
    var restoreId: String? = null
    var restoredOnce: String? = null
}
