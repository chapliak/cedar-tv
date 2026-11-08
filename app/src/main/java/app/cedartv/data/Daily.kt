package app.cedartv.data

import android.content.SharedPreferences
import android.text.Html
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.time.LocalDate

/** Today's daily text (the theme scripture) from the public online library, in the content language. */
object Daily {
    private val client = OkHttpClient()
    private const val UA = "Mozilla/5.0 (compatible; CedarTV/1.0)"
    private val THEME = Regex("""<p[^>]*class="[^"]*themeScrp[^"]*"[^>]*>(.*?)</p>""", RegexOption.DOT_MATCHES_ALL)
    private val BASE = Regex("""(https://wol\.jw\.org/[^/]+/wol)/h/(r\d+)/(lp-[a-z0-9-]+)""")

    private fun get(url: String): Response =
        client.newCall(Request.Builder().url(url).header("User-Agent", UA).build()).execute()

    /** ".../wol/dt/r15/lp-k" for a language: found once by following the library's own redirect. */
    private fun dtBase(prefs: SharedPreferences, lang: String): String? {
        prefs.getString("dtbase_$lang", null)?.let { return it }
        get("https://wol.jw.org/wol/finder?wtlocale=$lang").use { r ->
            val m = BASE.find(r.request.url.toString()) ?: return null
            val b = "${m.groupValues[1]}/dt/${m.groupValues[2]}/${m.groupValues[3]}"
            prefs.edit().putString("dtbase_$lang", b).apply()
            return b
        }
    }

    private fun fetchVerse(prefs: SharedPreferences, lang: String, day: LocalDate): String? {
        val base = dtBase(prefs, lang) ?: return null
        get("$base/${day.year}/${day.monthValue}/${day.dayOfMonth}").use { r ->
            if (!r.isSuccessful) return null
            val html = r.body?.string() ?: return null
            val m = THEME.find(html) ?: return null
            return Html.fromHtml(m.groupValues[1], Html.FROM_HTML_MODE_COMPACT).toString()
                .replace("​", "").replace(Regex("\\s+"), " ").trim().ifBlank { null }
        }
    }

    /** Today's verse; cached per language and day; falls back to English, then to the last one seen. */
    suspend fun get(prefs: SharedPreferences, lang: String): String? = withContext(Dispatchers.IO) {
        val day = LocalDate.now()
        val key = "dt_${lang}_$day"
        prefs.getString(key, null)?.let { return@withContext it }
        val v = try {
            fetchVerse(prefs, lang, day) ?: if (lang != "E") fetchVerse(prefs, "E", day) else null
        } catch (e: Exception) { null }
        if (v != null) prefs.edit().putString(key, v).putString("dt_last_$lang", v).apply()
        v ?: prefs.getString("dt_last_$lang", null)
    }
}
