package id.icantartil.app.audio

import java.text.Normalizer

/** Pengolah teks tafsir: normalisasi (saat disimpan), pemecah kalimat, dan teks siap-ucap. Murni Kotlin, bisa diuji tanpa Android. */
object TafsirText {
    /** Naikkan angka ini jika aturan normalisasi berubah; tafsir tersimpan akan dinormalkan ulang dari data mentah. */
    const val NORM_VERSION = 1

    private const val MIN_LEN = 25      // potongan lebih pendek digabung (mis. "No. 5.")
    private const val MAX_LEN = 1500    // batas aman untuk satu ucapan TTS

    private fun ci(p: String) = Regex(p, RegexOption.IGNORE_CASE)

    private val FEMALE_NAMES = listOf(
        "aisyah", "aisah", "aisha", "khadijah", "khodijah", "fatimah", "fathimah", "hafshah", "hafsah", "zainab",
        "zaenab", "ummu", "maryam", "asma", "shafiyyah", "juwairiyah", "ramlah", "saudah", "maimunah", "sumayyah", "halimah",
    )

    private val QS_REF = ci("""\(?\bQ\.?\s?S\.?\s*([^/()\d]+?)\s*/\s*\d+\s*:\s*(\d+)(?:\s*[-–]\s*(\d+))?\s*\)?""")
    private val QS_LEFT = ci("""\bQ\.\s?S\.""")
    private val HR = Regex("""\bH\.?\s?R\.?\s+(?=[A-Z])""")
    private val RA = ci("""\br\.\s?a\b\.?""")          // hanya bentuk bertitik; "ra" tanpa titik terlalu ambigu
    private val AS_ = ci("""\ba\.\s?s\b\.?""")
    private val RH = ci("""\br\.\s?h\b\.?""")
    private val FOOTNOTE = Regex("""\[\d+]""")

    /** Singkatan -> bentuk lengkap. */
    private val ABBR: List<Pair<Regex, String>> = listOf(
        ci("""\bs\.?\s?a\.?\s?w\b\.?""") to "shallallahu alaihi wasallam",
        ci("""\bs\.?\s?w\.?\s?t\b\.?""") to "subhanahu wa taala",
        ci("""\bs\.\s?d\b\.?""") to "sampai dengan",
        ci("""\bdll\b\.?""") to "dan lain-lain",
        ci("""\bdsb\b\.?""") to "dan sebagainya",
        ci("""\bdst\b\.?""") to "dan seterusnya",
        ci("""\bdkk\b\.?""") to "dan kawan-kawan",
        ci("""\byg\b\.?""") to "yang",
        ci("""\bdg\b\.?""") to "dengan",
        ci("""\btdk\b\.?""") to "tidak",
        ci("""\bdlm\b\.?""") to "dalam",
        ci("""\bsbg\b\.?""") to "sebagai",
        ci("""\bspt\b\.?""") to "seperti",
        ci("""\bkrn\b\.?""") to "karena",
        ci("""\butk\b\.?""") to "untuk",
        ci("""\btsb\b\.?""") to "tersebut",
    )

    /** Titik penutup singkatan dipertahankan hanya jika kalimat memang berakhir di situ. */
    private fun expand(text: String, re: Regex, full: (MatchResult) -> String): String = re.replace(text) { m ->
        val f = full(m)
        if (!m.value.endsWith(".")) f
        else {
            val rest = text.substring(m.range.last + 1).trimStart()
            if (rest.isEmpty() || rest[0].isUpperCase() || rest[0] in "\"'”’)") "$f." else f
        }
    }

    /** Mengubah singkatan (saw, swt, r.a., Q.S., HR, dll) menjadi bentuk lengkap agar terbaca benar dan tidak disingkat. */
    fun normalize(raw: String): String {
        var t = raw.replace(Regex("<[^>]*>"), " ").replace("&nbsp;", " ")
            .replace("\uFDFA", " shallallahu alaihi wasallam ").replace("\uFDFB", " jalla jalaluhu ")
        t = QS_REF.replace(t) { m ->
            val to = m.groupValues[3]
            "Quran surah ${m.groupValues[1].trim()} ayat ${m.groupValues[2]}" + if (to.isNotEmpty()) " sampai $to" else ""
        }
        t = QS_LEFT.replace(t, "Quran surah")
        t = HR.replace(t, "Hadis riwayat ")
        for ((re, full) in ABBR) t = expand(t, re) { full }
        t = expand(t, RA) { m ->
            val before = t.substring(maxOf(0, m.range.first - 30), m.range.first).lowercase()
            if (FEMALE_NAMES.any { before.contains(it) }) "radhiyallahu anha" else "radhiyallahu anhu"
        }
        t = expand(t, AS_) { "alaihissalam" }
        t = expand(t, RH) { "rahimahullah" }
        t = FOOTNOTE.replace(t, "")
        t = t.replace(Regex("[ \\t\\u00A0]+"), " ").replace(Regex("\\s+([.,;:!?])"), "$1")
        return t.trim()
    }

    private val ARABIC = Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF]+")

    /** Teks untuk diucapkan mesin suara Indonesia: tanpa aksara Arab dan tanda diakritik transliterasi. */
    fun speakable(s: String): String =
        Normalizer.normalize(ARABIC.replace(s, " "), Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("[\\u02BB\\u02BC\\u02BE\\u02BF\\u2018\\u2019\\u0060\\u00B4]"), "'")
            .replace(Regex("\\s+"), " ").trim()

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
