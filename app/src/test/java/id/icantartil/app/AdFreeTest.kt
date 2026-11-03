package id.icantartil.app

import id.icantartil.app.ads.AdFree
import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class AdFreeTest {
    private val wib = ZoneId.of("Asia/Jakarta")
    private fun at(h: Int, m: Int = 0) = ZonedDateTime.of(2026, 11, 3, h, m, 0, 0, wib).toInstant().toEpochMilli()
    private fun hm(ms: Long) = java.time.Instant.ofEpochMilli(ms).atZone(wib).let { "${it.dayOfMonth} ${it.hour}:${it.minute}" }

    @Test fun oneAdAt0600_gives12h_until1800() = assertEquals("3 18:0", hm(AdFree.expiry(at(6), AdFree.ONE_AD_HOURS, wib)))
    @Test fun oneAdAt2000_cappedAtMidnight() = assertEquals("4 0:0", hm(AdFree.expiry(at(20), AdFree.ONE_AD_HOURS, wib)))
    @Test fun twoAdsAt1000_cappedAtMidnight_not24h() = assertEquals("4 0:0", hm(AdFree.expiry(at(10), AdFree.TWO_ADS_HOURS, wib)))
    @Test fun twoAdsAt0000_staysAlmostFullDay() = assertEquals("4 0:0", hm(AdFree.expiry(at(0), AdFree.TWO_ADS_HOURS, wib)))
    @Test fun lateNight_capIsOneMinute() = assertEquals("4 0:0", hm(AdFree.expiry(at(23, 59), AdFree.TWO_ADS_HOURS, wib)))
    @Test fun statusResetsAfterMidnight() {
        val until = AdFree.expiry(at(10), AdFree.TWO_ADS_HOURS, wib)
        assertTrue(AdFree.isFree(until, at(23, 59)))
        assertFalse(AdFree.isFree(until, until))
    }
    @Test fun grantHours() {
        assertEquals(0L, AdFree.grantHours(0)); assertEquals(12L, AdFree.grantHours(1)); assertEquals(24L, AdFree.grantHours(2))
    }
}
