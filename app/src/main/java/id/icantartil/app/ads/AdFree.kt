package id.icantartil.app.ads

import java.time.Instant
import java.time.ZoneId

/**
 * Aturan bebas-iklan:
 *  - 1 iklan  -> 12 jam
 *  - 2 iklan  -> 24 jam
 *  - Berakhir paling lambat pukul 00.00 (waktu lokal) hari berikutnya; setelah itu iklan aktif lagi.
 */
object AdFree {
    const val ONE_AD_HOURS = 12L
    const val TWO_ADS_HOURS = 24L

    fun grantHours(adsWatched: Int): Long = when {
        adsWatched >= 2 -> TWO_ADS_HOURS
        adsWatched == 1 -> ONE_AD_HOURS
        else -> 0L
    }

    fun nextMidnight(nowMs: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate().plusDays(1)
            .atStartOfDay(zone).toInstant().toEpochMilli()

    fun expiry(nowMs: Long, hours: Long, zone: ZoneId = ZoneId.systemDefault()): Long =
        minOf(nowMs + hours * 3_600_000L, nextMidnight(nowMs, zone))

    fun isFree(untilMs: Long, nowMs: Long): Boolean = nowMs < untilMs
}
