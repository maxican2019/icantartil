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
}
