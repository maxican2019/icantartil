package id.icantartil.app.ui

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.icantartil.app.ads.AdFree
import id.icantartil.app.ads.Ads
import id.icantartil.app.audio.AudioEngine
import id.icantartil.app.audio.Narrator
import id.icantartil.app.data.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException

enum class Screen { Home, Range, Player, History, Storage }

class AppVm(app: Application) : AndroidViewModel(app) {
    private val repo = QuranRepository(app)

    var screen by mutableStateOf(Screen.Home)
    var chapters by mutableStateOf<List<Chapter>>(emptyList()); private set
    var loading by mutableStateOf(true); private set
    var error by mutableStateOf<String?>(null); private set
    var progress by mutableStateOf<Pair<Int, Int>?>(null); private set
    var chapterId by mutableIntStateOf(1); private set
    var from by mutableIntStateOf(1); private set
    var to by mutableIntStateOf(5); private set
    var loop by mutableStateOf(Loop()); private set
    var ayahs by mutableStateOf<List<Ayah>>(emptyList()); private set
    var queue by mutableStateOf<List<Ayah>>(emptyList()); private set
    var history by mutableStateOf(Prefs.history()); private set
    var adFreeUntil by mutableLongStateOf(Prefs.adFreeUntil); private set
    var now by mutableLongStateOf(System.currentTimeMillis()); private set
    var syncedAt by mutableLongStateOf(0L); private set
    var busy by mutableStateOf<String?>(null); private set
    var cacheTick by mutableIntStateOf(0); private set
    var toast by mutableStateOf<String?>(null)
    var tafsirView by mutableStateOf<Pair<String, String>?>(null); private set
    var reciter by mutableStateOf(Prefs.reciter); private set
    var showReciters by mutableStateOf(false)

    val isAdFree: Boolean get() = AdFree.isFree(adFreeUntil, now)
    fun chapter(id: Int) = chapters.firstOrNull { it.id == id }
    fun name(id: Int) = chapter(id)?.name ?: "Surat $id"

    init {
        viewModelScope.launch { boot() }
        viewModelScope.launch { while (true) { delay(20_000); now = System.currentTimeMillis() } }
    }

    private suspend fun boot() {
        loading = true; error = null
        try {
            repo.ensureLoaded { d, t -> progress = d to t }
            chapters = repo.chapters()
            syncedAt = repo.syncedAt()
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { error = describe(e) }
        progress = null; loading = false
    }

    fun retry() { viewModelScope.launch { boot() } }

    /** Pesan galat yang menyebut penyebab sebenarnya, supaya mudah didiagnosis. */
    private fun describe(e: Exception): String = when (e) {
        is HttpException -> "Server equran.id menjawab HTTP ${e.code}. Coba lagi nanti."
        is IOException -> "Tidak bisa terhubung ke equran.id (${e.javaClass.simpleName}). Periksa koneksi internet."
        else -> "Data tidak terbaca (${e.javaClass.simpleName}: ${e.message?.take(80)})."
    }

    private suspend fun guarded(fail: String, block: suspend () -> Unit) {
        try { block() } catch (e: CancellationException) { throw e
        } catch (e: Exception) { toast = if (e is IOException || e is HttpException) fail else "$fail (${e.javaClass.simpleName})" }
    }

    // ---- Pemilihan rentang ----
    fun openChapter(c: Chapter) {
        chapterId = c.id; from = 1; to = minOf(5, c.count); ayahs = emptyList(); screen = Screen.Range
        viewModelScope.launch { guarded("Gagal memuat ayat.") { ayahs = repo.ayahs(c.id, 1, c.count) } }
    }

    fun openRange() {
        if (chapters.isEmpty()) return
        if (ayahs.isEmpty()) openChapter(chapter(chapterId) ?: chapters.first()) else screen = Screen.Range
    }

    fun updateFrom(v: Int) { val c = chapter(chapterId) ?: return; from = v.coerceIn(1, c.count); if (to < from) to = from }
    fun updateTo(v: Int) { val c = chapter(chapterId) ?: return; to = v.coerceIn(from, c.count) }

    private val perAyahOpts = listOf(1, 2, 3, 5, 10)
    private val perRangeOpts = listOf(0, 2, 3, 5, 10)
    private val pauseOpts = listOf(0, 1, 2, 3, 5)
    private fun <T> nextOf(l: List<T>, v: T): T = l[(l.indexOf(v) + 1).mod(l.size)]
    fun cyclePerAyah() { loop = loop.copy(perAyah = nextOf(perAyahOpts, loop.perAyah)) }
    fun cycleRange() { loop = loop.copy(perRange = nextOf(perRangeOpts, loop.perRange)) }
    fun cyclePause() { loop = loop.copy(pauseSec = nextOf(pauseOpts, loop.pauseSec)) }

    // ---- Pemutaran ----
    private suspend fun start(list: List<Ayah>) {
        val playable = list.filter { it.audioUrl.isNotBlank() }
        if (playable.isEmpty()) { toast = "Audio ayat tidak tersedia"; return }
        queue = playable
        try {
            AudioEngine.prefetch(playable.map { it.audioUrl }) { busy = "Mengunduh audio $it/${playable.size}" }
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { /* lanjut: putar dari cache atau streaming */ }
        busy = null; cacheTick++
        Narrator.stop()
        AudioEngine.play(chapterId, playable, loop)
        Prefs.addHistory(HistoryItem(chapterId, playable.first().number, playable.last().number, loop, System.currentTimeMillis()))
        history = Prefs.history()
        screen = Screen.Player
    }

    fun playRange() {
        viewModelScope.launch {
            busy = "Menyiapkan…"
            guarded("Gagal memutar. Periksa koneksi.") { start(ayahs.filter { it.number in from..to }) }
            busy = null
        }
    }

    fun replay(h: HistoryItem) {
        chapterId = h.chapter; from = h.from; to = h.to; loop = h.loop; ayahs = emptyList()
        viewModelScope.launch {
            busy = "Menyiapkan…"
            guarded("Gagal memuat ayat.") { start(repo.ayahs(h.chapter, h.from, h.to)) }
            busy = null
        }
    }

    /** Terapkan ulang pengaturan loop pada rentang yang sedang diputar. */
    fun restart() { if (queue.isNotEmpty()) replay(HistoryItem(chapterId, queue.first().number, queue.last().number, loop, 0)) }

    /** Tombol Play di tengah menu: ulang rentang terakhir, atau jeda/lanjut jika sedang aktif. */
    fun center() {
        if (AudioEngine.state.value.active) { AudioEngine.toggle(); return }
        val h = history.firstOrNull()
        if (h != null) replay(h) else if (chapters.isNotEmpty()) openChapter(chapters.first())
    }

    fun downloadChapter() {
        viewModelScope.launch {
            val c = chapter(chapterId) ?: return@launch
            guarded("Gagal mengunduh. Periksa koneksi.") {
                val all = ayahs.ifEmpty { repo.ayahs(c.id, 1, c.count) }
                val urls = all.map { it.audioUrl }.filter { it.isNotBlank() }
                AudioEngine.prefetch(urls) { busy = "Mengunduh ${c.name} $it/${urls.size}" }
                toast = "Surat ${c.name} tersimpan di cache"
            }
            busy = null; cacheTick++
        }
    }

    fun chooseReciter(id: String) {
        Prefs.reciter = id; reciter = id; showReciters = false
        val c = chapter(chapterId)
        if (c != null && screen == Screen.Range) {
            viewModelScope.launch {
                busy = "Memuat qari…"
                guarded("Gagal memuat audio qari. Periksa koneksi.") { ayahs = repo.ayahs(c.id, 1, c.count) }
                busy = null
            }
        } else ayahs = emptyList()
    }

    fun clearHistory() { Prefs.clearHistory(); history = emptyList() }
    fun clearAudio() { AudioEngine.clearCache(); cacheTick++; toast = "Cache audio dibersihkan" }

    /** Unduh ulang seluruh teks dari server (opsional; data jarang berubah). */
    fun refreshData() {
        viewModelScope.launch {
            busy = "Memperbarui data…"
            guarded("Gagal memperbarui. Periksa koneksi.") {
                repo.refreshFromServer { d, t -> busy = "Memperbarui data $d/$t" }
                chapters = repo.chapters(); ayahs = emptyList(); syncedAt = repo.syncedAt()
                toast = "Data Al-Qur'an diperbarui"
            }
            busy = null
        }
    }

    // ---- Tafsir (diambil saat dibutuhkan, lalu disimpan) ----
    fun openTafsir(chapter: Int, ayah: Int) {
        viewModelScope.launch {
            busy = "Memuat tafsir…"
            guarded("Tafsir belum bisa dimuat. Periksa koneksi.") {
                val t = repo.tafsir(chapter)[ayah]
                tafsirView = "Tafsir ${name(chapter)} ayat $ayah" to
                    (if (t.isNullOrBlank()) "Tafsir untuk ayat ini tidak tersedia." else t)
            }
            busy = null
        }
    }

    fun closeTafsir() { tafsirView = null }

    // ---- Iklan ----
    /** count = 1 -> bebas iklan 12 jam; count = 2 -> 24 jam. Selalu berakhir paling lambat pukul 00.00. */
    fun watch(activity: Activity, count: Int) {
        viewModelScope.launch {
            var done = 0
            for (i in 1..count) {
                busy = "Memuat iklan $i/$count…"
                if (!Ads.show(activity)) break
                done++
            }
            busy = null
            if (done == 0) { toast = "Iklan belum tersedia. Coba lagi sebentar lagi."; return@launch }
            val t = AdFree.expiry(System.currentTimeMillis(), AdFree.grantHours(done))
            adFreeUntil = maxOf(adFreeUntil, t); Prefs.adFreeUntil = adFreeUntil
            now = System.currentTimeMillis()
            toast = "Terima kasih! Bebas iklan sampai pukul ${fmtTime(adFreeUntil)}"
        }
    }

    /** Menonton iklan secara sukarela untuk membantu developer (tanpa imbalan). */
    fun support(activity: Activity) {
        viewModelScope.launch {
            busy = "Memuat iklan…"
            val ok = Ads.show(activity)
            busy = null
            toast = if (ok) "Jazakumullahu khairan, terima kasih sudah mendukung developer." else "Iklan belum tersedia."
        }
    }
}
