package id.icantartil.app

import id.icantartil.app.audio.TafsirText
import org.junit.Assert.*
import org.junit.Test

class TafsirTextTest {
    @Test fun splitsIntoSentences() {
        val r = TafsirText.split("Surat ini diturunkan di Mekah sebelum hijrah. Ia disebut pembuka kitab! Apakah engkau paham maksudnya?")
        assertEquals(3, r.size)
    }
    @Test fun shortFragmentsAreMerged() {
        val r = TafsirText.split("Lihat Q.S. Al-Baqarah ayat 255 untuk penjelasan lebih lanjut. Selesai dibahas.")
        assertTrue(r.none { it.length < 10 })
        assertTrue(r.first().startsWith("Lihat Q.S."))
    }
    @Test fun stripsHtmlAndKeepsParagraphs() {
        val r = TafsirText.split("<p>Paragraf pertama yang cukup panjang untuk dibaca.</p>\nParagraf kedua yang juga cukup panjang.")
        assertEquals(2, r.size)
        assertFalse(r.any { it.contains("<") })
    }
    @Test fun emptyAndLong() {
        assertTrue(TafsirText.split("   ").isEmpty())
        assertTrue(TafsirText.split("a".repeat(4000)).all { it.length <= 1500 })
    }

    @Test fun normalizeExpandsHonorifics() {
        assertEquals("Nabi Muhammad shallallahu alaihi wasallam bersabda.", TafsirText.normalize("Nabi Muhammad saw. bersabda."))
        assertEquals("Allah subhanahu wa taala. Dialah Tuhan.", TafsirText.normalize("Allah swt. Dialah Tuhan."))
        assertEquals("Aisyah radhiyallahu anha meriwayatkan.", TafsirText.normalize("Aisyah r.a. meriwayatkan."))
    }
    @Test fun normalizeDoesNotTouchOrdinaryWords() {
        val s = "Petani menanam di sawah. Perusahaan swasta ikut."
        assertEquals(s, TafsirText.normalize(s))
    }
    @Test fun normalizeQuranReference() {
        assertEquals("Lihat Quran surah Ali Imran ayat 102 sampai 103.", TafsirText.normalize("Lihat Q.S. Ali Imran/3: 102-103."))
    }
    @Test fun speakableRemovesArabicScript() {
        val s = TafsirText.speakable("Allah berfirman: \u0628\u0650\u0633\u0652\u0645\u0650 dan Rahm\u0101n.")
        assertTrue(s.none { it in '\u0600'..'\u06FF' })
        assertTrue(s.contains("Rahman"))
    }
}
