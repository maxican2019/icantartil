package id.icantartil.app.audio

import android.app.Application
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.core.content.ContextCompat
import id.icantartil.app.data.Prefs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * unit = indeks ayat dalam daftar yang dibacakan, idx = indeks kalimat di dalam ayat itu.
 * auto = lanjut otomatis ke ayat berikutnya setelah audio ayat selesai.
 */
data class NarratorState(
    val speaking: Boolean = false, val unit: Int = -1, val idx: Int = -1, val rate: Float = 1f,
    val female: Boolean = true, val auto: Boolean = true, val error: String? = null,
)

/**
 * Pembaca tafsir memakai suara sintetis bawaan Android (TextToSpeech, bahasa Indonesia).
 * Gratis dan tanpa kunci API; bisa offline jika data suara terpasang. Android tidak menyediakan jenis kelamin
 * suara, jadi suara wanita/pria ditetapkan lewat nama suara (FEMALE_VOICE/MALE_VOICE). Jika suara itu tidak ada
 * di HP, dipakai tebakan berdasarkan urutan suara plus penyesuaian nada.
 */
object Narrator {
    /** Suara bawaan yang dipilih pengembang (Google TTS Indonesia). */
    private const val FEMALE_VOICE = "id-id-x-idc-local"
    private const val MALE_VOICE = "id-id-x-ide-local"

    private lateinit var app: Application
    private var tts: TextToSpeech? = null
    private var ready = false
    private var serviceOn = false
    private var units: List<List<String>> = emptyList()
    private val rates = listOf(0.8f, 1f, 1.25f)
    private val main = Handler(Looper.getMainLooper())
    private val idRegex = Regex("u(\\d+)s(\\d+)")
    private val _state = MutableStateFlow(NarratorState())
    val state: StateFlow<NarratorState> = _state.asStateFlow()

    fun init(a: Application) {
        app = a
        _state.value = NarratorState(female = Prefs.tafsirFemale, auto = Prefs.tafsirAuto)
    }

    fun rateLabel(r: Float): String = when (r) { 0.8f -> "0,8×"; 1.25f -> "1,25×"; else -> "1×" }

    private fun parse(id: String?): Pair<Int, Int>? {
        val m = idRegex.matchEntire(id ?: return null) ?: return null
        return m.groupValues[1].toInt() to m.groupValues[2].toInt()
    }

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            parse(utteranceId)?.let { (u, s) -> _state.value = _state.value.copy(speaking = true, unit = u, idx = s, error = null) }
        }
        override fun onDone(utteranceId: String?) {
            val (u, s) = parse(utteranceId) ?: return
            if (units.getOrNull(u)?.lastIndex != s) return
            main.post {
                val st = _state.value
                if (st.auto && u + 1 < units.size) speakUnit(u + 1, 0)
                else { _state.value = st.copy(speaking = false, idx = -1); serviceDown() }
            }
        }
        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            _state.value = _state.value.copy(speaking = false, error = "Pembacaan gagal. Coba lagi.")
        }
    }

    private fun ensure(done: (Boolean) -> Unit) {
        if (ready && tts != null) { done(true); return }
        tts = TextToSpeech(app) { status ->
            val t = tts
            var ok = false
            if (status == TextToSpeech.SUCCESS && t != null) {
                val r = t.setLanguage(Locale("id", "ID"))
                ok = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED
                t.setOnUtteranceProgressListener(listener)
            }
            ready = ok
            if (!ok) _state.value = _state.value.copy(
                speaking = false,
                error = "Suara Bahasa Indonesia belum terpasang. Buka Pengaturan HP > Teks-ke-ucapan, lalu pasang data suara Indonesia.",
            )
            done(ok)
        }
    }

    private fun idVoices(t: TextToSpeech): List<Voice> = try {
        (t.voices ?: emptySet()).filter { it.locale.language == "id" || it.locale.language == "in" }.sortedBy { it.name }
    } catch (e: Exception) { emptyList() }

    private fun applyVoice(t: TextToSpeech) {
        val female = _state.value.female
        val vs = idVoices(t)
        var v = vs.firstOrNull { it.name == (if (female) FEMALE_VOICE else MALE_VOICE) }
        var pitch = 1f
        if (v == null) { // suara bawaan tidak ada di HP ini: tebakan urutan + penyesuaian nada
            v = if (vs.size >= 2) (if (female) vs[0] else vs[1]) else vs.firstOrNull()
            pitch = if (female) 1.1f else 0.85f
        }
        if (v != null) t.setVoice(v)
        t.setPitch(pitch)
    }

    private fun speakUnit(u: Int, from: Int) {
        val t = tts ?: return
        val sents = units.getOrNull(u) ?: return
        if (sents.isEmpty()) { // ayat tanpa tafsir: lewati
            if (_state.value.auto && u + 1 < units.size) speakUnit(u + 1, 0)
            else _state.value = _state.value.copy(speaking = false, unit = u, idx = -1)
            return
        }
        AudioEngine.player.pause() // jeda lantunan agar tidak bertabrakan
        t.stop(); applyVoice(t); t.setSpeechRate(_state.value.rate)
        for (k in from until sents.size) {
            val spoken = TafsirText.speakable(sents[k]) // aksara Arab tidak dibacakan oleh suara Indonesia
            if (spoken.isBlank()) t.playSilentUtterance(150, TextToSpeech.QUEUE_ADD, "u${u}s$k")
            else t.speak(spoken, TextToSpeech.QUEUE_ADD, null, "u${u}s$k")
        }
        serviceUp()
        _state.value = _state.value.copy(speaking = true, unit = u, idx = from, error = null)
    }

    private fun serviceUp() {
        if (serviceOn) return
        serviceOn = true
        try { ContextCompat.startForegroundService(app, Intent(app, NarrationService::class.java)) } catch (e: Exception) { serviceOn = false }
    }

    private fun serviceDown() {
        if (!serviceOn) return
        serviceOn = false
        app.stopService(Intent(app, NarrationService::class.java))
    }

    /** Muat daftar ayat (tiap ayat = daftar kalimat) yang akan dibacakan. */
    fun load(list: List<List<String>>) { stop(); units = list }

    fun play(unit: Int, from: Int = 0) { ensure { ok -> if (ok) speakUnit(unit, from) } }
    fun pause() { tts?.stop(); _state.value = _state.value.copy(speaking = false); serviceDown() }
    fun resume() { val s = _state.value; play(s.unit.coerceAtLeast(0), s.idx.coerceAtLeast(0)) }
    fun next() { val u = _state.value.unit + 1; if (u in units.indices) play(u) }
    fun prev() { val u = _state.value.unit - 1; if (u in units.indices) play(u) }
    fun stop() { tts?.stop(); _state.value = _state.value.copy(speaking = false, unit = -1, idx = -1); serviceDown() }

    fun toggleAuto() { val a = !_state.value.auto; Prefs.tafsirAuto = a; _state.value = _state.value.copy(auto = a) }

    fun useFemale(female: Boolean) {
        Prefs.tafsirFemale = female
        _state.value = _state.value.copy(female = female)
        val s = _state.value
        if (s.speaking) speakUnit(s.unit.coerceAtLeast(0), s.idx.coerceAtLeast(0))
    }

    fun cycleRate() {
        val next = rates[(rates.indexOf(_state.value.rate) + 1).mod(rates.size)]
        _state.value = _state.value.copy(rate = next)
        val s = _state.value
        if (s.speaking) speakUnit(s.unit.coerceAtLeast(0), s.idx.coerceAtLeast(0))
    }
}
