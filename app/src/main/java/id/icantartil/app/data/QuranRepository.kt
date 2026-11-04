package id.icantartil.app.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.IOException

/**
 * Alur data:
 *  1. Teks 114 surat disimpan di SQLite lokal, dibaca dari lokal (cepat, offline).
 *  2. Pertama kali: impor bundel bawaan APK (assets/quran_bundle.json) jika ada; jika tidak, unduh dari equran.id.
 *  3. Audio: URL dari API, diputar streaming + cache (lihat AudioEngine).
 *  4. Tafsir: diambil saat dibutuhkan lalu disimpan di database.
 */
class QuranRepository(private val ctx: Context) {
    private val db = QuranDb(ctx)

    suspend fun chapters(): List<Chapter> = withContext(Dispatchers.IO) { db.chapters() }

    /** Teks dari database lokal; URL audio sesuai qari terpilih (EQuran.id atau Al Quran Cloud). */
    suspend fun ayahs(chapter: Int, from: Int, to: Int): List<Ayah> {
        val r = Reciters.byId(Prefs.reciter)
        val rows = withContext(Dispatchers.IO) { db.ayahs(chapter, from, to, if (r.source == Reciters.EQ) r.key else "05") }
        if (r.source == Reciters.EQ) return rows
        val count = withContext(Dispatchers.IO) { db.ayahCount(chapter) }
        val urls = cloudUrls(r, chapter, count)
        return rows.map { it.copy(audioUrl = urls[it.number] ?: "") }
    }

    /**
     * URL audio per ayat dari api.alquran.cloud (sudah memuat bitrate yang benar), disimpan di database.
     * Jika API gagal, dipakai pola CDN: cdn.islamic.network/quran/audio/128/{edisi}/{nomor ayat global}.mp3
     * (pola cadangan ini tidak disimpan, supaya tidak mengunci URL yang mungkin salah bitrate).
     */
    private suspend fun cloudUrls(r: Reciter, surat: Int, count: Int): Map<Int, String> {
        val cached = withContext(Dispatchers.IO) { db.cloudUrls(r.id, surat) }
        if (cached.size >= count) return cached
        val map = LinkedHashMap<Int, String>()
        try {
            val arr = getRetry(CloudApi, "/surah/$surat/${r.key}").getJSONObject("data").getJSONArray("ayahs")
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                var u = o.str("audio")
                if (u.isBlank()) o.optJSONArray("audioSecondary")?.let { if (it.length() > 0) u = it.getString(0) }
                if (u.isNotBlank()) map[o.getInt("numberInSurah")] = if (u.startsWith("http://")) "https://" + u.removePrefix("http://") else u
            }
        } catch (e: CancellationException) { throw e } catch (e: Exception) { /* pakai pola CDN di bawah */ }
        if (map.size >= count) {
            withContext(Dispatchers.IO) { db.putCloudUrls(r.id, surat, map) }
            return map
        }
        val g = withContext(Dispatchers.IO) { db.globalStart(surat) }
        for (n in 1..count) map.putIfAbsent(n, "https://cdn.islamic.network/quran/audio/128/${r.key}/${g + n}.mp3")
        return map
    }

    suspend fun syncedAt(): Long = withContext(Dispatchers.IO) { db.getMeta("synced_at")?.toLongOrNull() ?: 0L }

    suspend fun ensureLoaded(onProgress: (Int, Int) -> Unit) {
        if (withContext(Dispatchers.IO) { db.isComplete() }) return
        val imported = withContext(Dispatchers.IO) { importBundle() && db.isComplete() }
        if (imported) return
        download(onProgress, onlyMissing = true)
    }

    suspend fun refreshFromServer(onProgress: (Int, Int) -> Unit) = download(onProgress, onlyMissing = false)

    private fun importBundle(): Boolean {
        return try {
            val text = ctx.assets.open("quran_bundle.json").bufferedReader().use { it.readText() }
            val root = JSONObject(text)
            db.putSurat(root.getJSONArray("list"))
            val detail = root.getJSONObject("detail")
            val keys = detail.keys()
            while (keys.hasNext()) { val k = keys.next(); db.putAyatJson(k.toInt(), detail.getJSONArray(k)) }
            true
        } catch (e: Exception) { false }
    }

    private suspend fun getRetry(path: String): JSONObject = getRetry(null, path)

    private suspend fun getRetry(cloud: CloudApi?, path: String): JSONObject {
        var last: Exception? = null
        for (i in 0 until 3) {
            try { return if (cloud != null) cloud.get(path) else EQuranApi.get(path) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { last = e; delay(800L * (i + 1)) }
        }
        throw last ?: IOException("Gagal mengambil $path")
    }

    private suspend fun download(onProgress: (Int, Int) -> Unit, onlyMissing: Boolean) {
        val list = getRetry("/surat").getJSONArray("data")
        withContext(Dispatchers.IO) { db.putSurat(list) }
        val have: Set<Int> = if (onlyMissing) withContext(Dispatchers.IO) { db.loadedAyahSurahs() } else emptySet()
        val todo = (0 until list.length()).map { list.getJSONObject(it).getInt("nomor") }.filter { it !in have }
        for ((i, n) in todo.withIndex()) {
            val data = getRetry("/surat/$n").getJSONObject("data")
            withContext(Dispatchers.IO) { db.putAyatJson(n, data.getJSONArray("ayat")) }
            onProgress(i + 1, todo.size)
            delay(100) // ramah ke server
        }
        withContext(Dispatchers.IO) { db.setMeta("synced_at", System.currentTimeMillis().toString()) }
    }

    /** Tafsir per surat, diambil sekali lalu disimpan. @return nomor ayat -> teks tafsir. */
    suspend fun tafsir(chapter: Int): Map<Int, String> {
        val cached = withContext(Dispatchers.IO) { db.tafsirJson(chapter) }
        if (cached != null) return parseTafsir(JSONObject(cached))
        val data = getRetry("/tafsir/$chapter").getJSONObject("data")
        withContext(Dispatchers.IO) { db.putTafsir(chapter, data.toString()) }
        return parseTafsir(data)
    }

    private fun parseTafsir(d: JSONObject): Map<Int, String> {
        val a = d.optJSONArray("tafsir") ?: return emptyMap()
        val m = LinkedHashMap<Int, String>()
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            m[o.optInt("ayat", i + 1)] = o.str("teks")
        }
        return m
    }
}
