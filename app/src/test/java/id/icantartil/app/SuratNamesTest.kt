package id.icantartil.app

import id.icantartil.app.data.SuratNames
import org.junit.Assert.*
import org.junit.Test

class SuratNamesTest {
    @Test fun has114UniqueNames() {
        assertEquals(114, SuratNames.size)
        val all = (1..114).map { SuratNames.arabic(it)!! }
        assertEquals(114, all.toSet().size)
    }
    @Test fun hamzaIsWritten() {
        assertEquals("الإخلاص", SuratNames.arabic(112))
        assertEquals("الأنعام", SuratNames.arabic(6))
        assertEquals("إبراهيم", SuratNames.arabic(14))
        assertEquals("الإنسان", SuratNames.arabic(76))
        assertEquals("آل عمران", SuratNames.arabic(3))
        assertEquals("النبأ", SuratNames.arabic(78))
    }
    @Test fun outOfRangeIsNull() {
        assertNull(SuratNames.arabic(0)); assertNull(SuratNames.arabic(115))
    }
}
