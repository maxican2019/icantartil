package id.icantartil.app.audio

import android.app.Application
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class NarratorState(val speaking: Boolean = false, val idx: Int = -1, val rate: Float = 1f, val error: String? = null)

/**
 * Pembaca tafsir memakai suara sintetis bawaan Android (TextToSpeech, bahasa Indonesia).
 * Gratis dan tanpa kunci API; bisa offline jika data suara sudah terpasang di HP.
 * Jeda = berhenti di kalimat itu; Lanjut = mengulang dari awal kalimat tersebut.
 */
object Narrator {
    private lateinit var app: Application
    private var tts: TextToSpeech? = null
    private var ready = false
    private var list: List<String> = emptyList()
    private val rates = listOf(0.8f, 1f, 1.25f)
    private val _state = MutableStateFlow(NarratorState())
    val state: StateFlow<NarratorState> = _state.asStateFlow()

    fun init(a: Application) { app = a }

    fun rateLabel(r: Float): String = when (r) { 0.8f -> "0,8×"; 1.25f -> "1,25×"; else -> "1×" }

    private fun idxOf(id: String?): Int? = id?.removePrefix("s")?.toIntOrNull()

    private val listener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            idxOf(utteranceId)?.let { _state.value = _state.value.copy(speaking = true, idx = it, error = null) }
        }
        override fun onDone(utteranceId: String?) {
            if (idxOf(utteranceId) == list.lastIndex) _state.value = _state.value.copy(speaking = false, idx = -1)
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

    private fun speakFrom(i: Int) {
        ensure { ok ->
            val t = tts
            if (!ok || t == null || list.isEmpty()) return@ensure
            AudioEngine.player.pause() // jeda lantunan agar tidak bertabrakan
            t.stop(); t.setSpeechRate(_state.value.rate)
            for (k in i until list.size) t.speak(list[k], TextToSpeech.QUEUE_ADD, null, "s$k")
            _state.value = _state.value.copy(speaking = true, idx = i, error = null)
        }
    }

    fun play(sentences: List<String>) { list = sentences; speakFrom(0) }
    fun pause() { tts?.stop(); _state.value = _state.value.copy(speaking = false) }
    fun resume() { speakFrom(_state.value.idx.coerceAtLeast(0)) }
    fun stop() { tts?.stop(); _state.value = NarratorState(rate = _state.value.rate) }

    fun cycleRate() {
        val next = rates[(rates.indexOf(_state.value.rate) + 1).mod(rates.size)]
        _state.value = _state.value.copy(rate = next)
        if (_state.value.speaking) speakFrom(_state.value.idx.coerceAtLeast(0))
    }
}
