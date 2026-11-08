package app.cedartv.data

data class Cue(val start: Long, val end: Long, val text: String)

/** Minimal WebVTT reader: cue times + plain text (tags and positioning stripped). */
object Vtt {
    private val ARROW = Regex("""((?:\d+:)?\d{1,2}:\d{2}[.,]\d{3})\s*-->\s*((?:\d+:)?\d{1,2}:\d{2}[.,]\d{3})""")
    private val TS = Regex("""(?:(\d+):)?(\d{1,2}):(\d{2})[.,](\d{3})""")
    private val TAGS = Regex("<[^>]+>")

    private fun ms(s: String): Long {
        val g = TS.find(s)!!.groupValues
        return (g[1].toLongOrNull() ?: 0) * 3_600_000 + g[2].toLong() * 60_000 + g[3].toLong() * 1000 + g[4].toLong()
    }

    private fun clean(s: String) = s.replace(TAGS, "")
        .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ")

    fun parse(text: String): List<Cue> {
        val out = ArrayList<Cue>()
        text.replace("\r", "").split("\n\n").forEach { block ->
            val lines = block.lines()
            val i = lines.indexOfFirst { "-->" in it }
            if (i < 0) return@forEach
            val m = ARROW.find(lines[i]) ?: return@forEach
            val txt = lines.drop(i + 1).joinToString("\n") { clean(it) }.trim()
            if (txt.isNotEmpty()) out += Cue(ms(m.groupValues[1]), ms(m.groupValues[2]), txt)
        }
        return out.sortedBy { it.start }
    }

    /** Index of the last cue that has started at time [t] (ms), or -1. */
    fun indexAt(cues: List<Cue>, t: Long): Int {
        var lo = 0; var hi = cues.size - 1; var idx = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (cues[mid].start <= t) { idx = mid; lo = mid + 1 } else hi = mid - 1
        }
        return idx
    }

    /** Text shown at time [t] (ms), or null. Binary search; looks back a few cues for overlaps. */
    fun at(cues: List<Cue>, t: Long): String? {
        var lo = 0; var hi = cues.size - 1; var idx = -1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            if (cues[mid].start <= t) { idx = mid; lo = mid + 1 } else hi = mid - 1
        }
        var i = idx; var n = 0
        while (i >= 0 && n < 3) { if (t < cues[i].end) return cues[i].text; i--; n++ }
        return null
    }
}
