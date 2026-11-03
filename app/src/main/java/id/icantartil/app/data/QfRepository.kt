package id.icantartil.app.data

import android.content.Context
import id.icantartil.app.BuildConfig
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.io.File

data class Chapter(val id: Int, val name: String, val arabic: String, val count: Int)
data class Ayah(val number: Int, val arabic: String, val audioUrl: String)

class ContentExpired : Exception("Data lisensi kedaluwarsa, butuh internet untuk memperbarui")

/** Batas waktu penyimpanan konten Quran Foundation (lihat README, bagian Kepatuhan). */
object Policy {
    const val DAY = 24L * 3600 * 1000
    const val REFRESH_AGE = 6 * DAY   // cache API biasa dianggap basi, ambil ulang
    const val MAX_AGE = 7 * DAY       // batas keras penyimpanan; lewat ini dihapus
    const val SYNC_DUE = 3 * DAY      // sinkronisasi audio dijalankan jika lebih tua dari ini
}

class QfRepository(ctx: Context) {
    private val dir = File(ctx.cacheDir, "qf").apply { mkdirs() }
    private val per = 50

    private suspend fun cached(name: String, path: String): JSONObject {
        val f = File(dir, name)
        val has = f.exists()
        val now = System.currentTimeMillis()
        val age = if (has) now - f.lastModified() else Long.MAX_VALUE
        if (has && age < Policy.REFRESH_AGE) return JSONObject(f.readText())
        try {
            val j = QfClient.getJson(path)
            f.writeText(j.toString()); f.setLastModified(now)
            return j
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (has && age <= Policy.MAX_AGE) return JSONObject(f.readText())
            if (has) { f.delete(); throw ContentExpired() }
            throw e
        }
    }

    suspend fun chapters(): List<Chapter> {
        val a = cached("chapters.json", "/content/api/v4/chapters?language=id").getJSONArray("chapters")
        return (0 until a.length()).map { i ->
            val o = a.getJSONObject(i)
            Chapter(o.getInt("id"), o.str("name_simple"), o.str("name_arabic"), o.getInt("verses_count"))
        }
    }

    suspend fun ayahs(chapter: Int, from: Int, to: Int): List<Ayah> {
        val out = ArrayList<Ayah>()
        for (p in ((from - 1) / per + 1)..((to - 1) / per + 1)) {
            val path = "/content/api/v4/verses/by_chapter/$chapter?language=id&words=false" +
                "&fields=text_uthmani&audio=${BuildConfig.RECITATION_ID}&per_page=$per&page=$p"
            val a = cached("v_${chapter}_$p.json", path).getJSONArray("verses")
            for (i in 0 until a.length()) {
                val o = a.getJSONObject(i)
                val n = o.getInt("verse_number")
                if (n in from..to) out += Ayah(n, o.str("text_uthmani"), audioUrl(o.optJSONObject("audio")?.str("url").orEmpty()))
            }
        }
        return out
    }

    suspend fun wholeChapter(c: Chapter) = ayahs(c.id, 1, c.count)

    private fun audioUrl(raw: String): String = when {
        raw.isBlank() -> ""
        raw.startsWith("http") -> raw
        raw.startsWith("//") -> "https:$raw"
        else -> "https://verses.quran.foundation/" + raw.trimStart('/')
    }

    fun purgeAll() { dir.listFiles()?.forEach { it.delete() } }
    fun purgeExpired() {
        val now = System.currentTimeMillis()
        dir.listFiles()?.filter { now - it.lastModified() > Policy.MAX_AGE }?.forEach { it.delete() }
    }
}
