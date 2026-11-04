package id.icantartil.app.audio

/** Memecah teks tafsir menjadi kalimat untuk dibacakan dan disorot. Murni Kotlin, bisa diuji tanpa Android. */
object TafsirText {
    private const val MIN_LEN = 25      // potongan lebih pendek digabung (mis. "Q.S." atau "No. 5.")
    private const val MAX_LEN = 1500    // batas aman untuk satu ucapan TTS

    fun split(raw: String): List<String> {
        val clean = raw.replace(Regex("<[^>]*>"), " ").replace("&nbsp;", " ")
            .replace(Regex("[ \\t\\u00A0]+"), " ").trim()
        if (clean.isEmpty()) return emptyList()
        val out = ArrayList<String>()
        for (para in clean.split(Regex("\\s*\\n+\\s*"))) {
            var buf = StringBuilder()
            for (piece in para.split(Regex("(?<=[.!?…])\\s+"))) {
                if (piece.isBlank()) continue
                if (buf.isNotEmpty()) buf.append(' ')
                buf.append(piece.trim())
                if (buf.length >= MIN_LEN) { out += buf.toString(); buf = StringBuilder() }
            }
            if (buf.isNotEmpty()) {
                if (out.isNotEmpty() && buf.length < MIN_LEN) out[out.lastIndex] = out.last() + " " + buf else out += buf.toString()
            }
        }
        return out.flatMap { if (it.length > MAX_LEN) it.chunked(MAX_LEN) else listOf(it) }
    }
}
