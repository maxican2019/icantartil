package id.icantartil.app.sync

import android.content.Context
import id.icantartil.app.BuildConfig
import id.icantartil.app.audio.AudioEngine
import id.icantartil.app.data.*
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.net.URLEncoder

/**
 * Alat kepatuhan lisensi Quran Foundation.
 * - Memakai Content Sync (resources/sync) untuk grup `recitations` sebagai change-feed audio.
 * - Jika ada perubahan (atau token hilang), cache audio dihapus supaya diunduh ulang.
 * - Dijalankan harian (WorkManager) dan saat aplikasi dibuka bila sudah lewat SYNC_DUE.
 * - Jika sudah lewat 7 hari dan server MENOLAK (bukan sekadar offline), semua cache dihapus.
 */
object ContentSync {
    sealed interface State {
        data object Fresh : State
        data object Offline : State
        data object Purged : State
        data class Failed(val code: Int) : State
    }

    private fun start(token: String?, filter: String) =
        "/content/api/v4/resources/sync?" +
            (if (token == null) "bootstrap=true" else "sync_token=" + URLEncoder.encode(token, "UTF-8")) +
            "&resources=" + URLEncoder.encode(filter, "UTF-8") + "&per_page=100"

    /** @return true jika audio di cache harus dianggap tidak valid. */
    private suspend fun run(): Boolean {
        val filter = "recitations:${BuildConfig.RECITATION_ID}"
        var token = Prefs.syncToken.takeIf { Prefs.syncFilter == filter }
        var invalidated = false
        var tries = 0
        while (true) {
            try {
                var url = start(token, filter)
                var changed = false
                var next: String? = null
                while (true) {
                    val sync = QfClient.getJson(url).getJSONObject("sync")
                    val m = sync.optJSONArray("mutations")
                    if (token != null && m != null && m.length() > 0) changed = true
                    if (sync.optBoolean("has_more", false)) {
                        url = "/content" + sync.getString("next_page_url")
                    } else {
                        next = if (sync.isNull("next_sync_token")) null else sync.getString("next_sync_token")
                        break
                    }
                }
                if (next != null) { Prefs.syncToken = next; Prefs.syncFilter = filter }
                Prefs.lastSyncAt = System.currentTimeMillis()
                return invalidated || changed
            } catch (e: HttpException) {
                // Token tidak valid / tidak cocok dengan filter: bootstrap ulang, cache audio dibuang.
                if (tries++ == 0 && token != null && e.code in intArrayOf(400, 410, 422)) {
                    token = null; Prefs.syncToken = null; invalidated = true
                } else throw e
            }
        }
    }

    suspend fun syncIfDue(ctx: Context, force: Boolean = false): State {
        val age = System.currentTimeMillis() - Prefs.lastSyncAt
        if (!force && age < Policy.SYNC_DUE) return State.Fresh
        return try {
            if (run()) AudioEngine.clearCache()
            QfRepository(ctx).purgeExpired()
            State.Fresh
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            State.Offline // tanpa koneksi: salinan lama boleh dipakai
        } catch (e: Exception) {
            if (age > Policy.MAX_AGE) {
                QfRepository(ctx).purgeAll(); AudioEngine.clearCache(); State.Purged
            } else State.Failed((e as? HttpException)?.code ?: 0)
        }
    }
}
