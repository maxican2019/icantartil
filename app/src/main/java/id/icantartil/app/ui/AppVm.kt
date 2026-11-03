package id.icantartil.app.ui

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import id.icantartil.app.ads.AdFree
import id.icantartil.app.ads.Ads
import id.icantartil.app.audio.AudioEngine
import id.icantartil.app.data.*
import id.icantartil.app.sync.ContentSync
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class Screen { Home, Range, Player, History, Storage }

class AppVm(app: Application) : AndroidViewModel(app) {
    private val repo = QfRepository(app)

    var screen by mutableStateOf(Screen.Home)
    var chapters by mutableStateOf<List<Chapter>>(emptyList()); private set
    var loading by mutableStateOf(true); private set
    var error by mutableStateOf<String?>(null); private set
    var chapterId by mutableIntStateOf(1); private set
    var from by mutableIntStateOf(1); private set
    var to by mutableIntStateOf(5); private set
    var loop by mutableStateOf(Loop()); private set
    var ayahs by mutableStateOf<List<Ayah>>(emptyList()); private set
    var queue by mutableStateOf<List<Ayah>>(emptyList()); private set
    var history by mutableStateOf(Prefs.history()); private set
    var adFreeUntil by mutableLongStateOf(Prefs.adFreeUntil); private set
    var now by mutableLongStateOf(System.currentTimeMillis()); private set
    var syncedAt by mutableLongStateOf(Prefs.lastSyncAt); private set
    var busy by mutableStateOf<String?>(null); private set
    var cacheTick by mutableIntStateOf(0); private set
    var toast by mutableStateOf<String?>(null)

    val isAdFree: Boolean get() = AdFree.isFree(adFreeUntil, now)
    fun chapter(id: Int) = chapters.firstOrNull { it.id == id }
    fun name(id: Int) = chapter(id)?.name ?: "Surat $id"

    init {
        viewModelScope.launch { boot() }
        viewModelScope.launch { while (true) { delay(20_000); now = System.currentTimeMillis() } }
    }

    private suspend fun boot() {
        loading = true; error = null
        ContentSync.syncIfDue(getApplication<Application>())
        syncedAt = Prefs.lastSyncAt
        try {
            chapters = repo.chapters()
        } catch (e: CancellationException) { throw e
        } catch (e: ContentExpired) { error = "Data perlu diperbarui. Sambungkan internet lalu coba lagi."
        } catch (e: Exception) { error = "Gagal memuat daftar surat. Periksa koneksi lalu coba lagi." }
        loading = false
    }

    fun retry() { viewModelScope.launch { boot() } }

    private suspend fun guarded(fail: String, block: suspend () -> Unit) {
        try { block() } catch (e: CancellationException) { throw e
        } catch (e: ContentExpired) { toast = "Data perlu diperbarui. Sambungkan internet."
        } catch (e: Exception) { toast = fail }
    }

    // ---- Pemilihan rentang ----
    fun openChapter(c: Chapter) {
        chapterId = c.id; from = 1; to = minOf(5, c.count); ayahs = emptyList(); screen = Screen.Range
        viewModelScope.launch { guarded("Gagal memuat ayat. Periksa koneksi.") { ayahs = repo.wholeChapter(c) } }
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
            guarded("Gagal memuat ayat. Periksa koneksi.") { start(repo.ayahs(h.chapter, h.from, h.to)) }
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
                val all = ayahs.ifEmpty { repo.wholeChapter(c) }
                val urls = all.map { it.audioUrl }.filter { it.isNotBlank() }
                AudioEngine.prefetch(urls) { busy = "Mengunduh ${c.name} $it/${urls.size}" }
                toast = "Surat ${c.name} tersimpan di cache"
            }
            busy = null; cacheTick++
        }
    }

    fun clearHistory() { Prefs.clearHistory(); history = emptyList() }
    fun clearAudio() { AudioEngine.clearCache(); cacheTick++; toast = "Cache audio dibersihkan" }

    fun syncNow() {
        viewModelScope.launch {
            busy = "Menyinkronkan…"
            val s = ContentSync.syncIfDue(getApplication<Application>(), force = true)
            syncedAt = Prefs.lastSyncAt; busy = null; cacheTick++
            toast = when (s) {
                ContentSync.State.Fresh -> "Sinkronisasi selesai"
                ContentSync.State.Offline -> "Tidak ada koneksi"
                ContentSync.State.Purged -> "Cache dibersihkan sesuai aturan lisensi"
                is ContentSync.State.Failed -> "Sinkronisasi gagal (${s.code})"
            }
        }
    }

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
