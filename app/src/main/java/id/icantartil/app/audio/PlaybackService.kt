package id.icantartil.app.audio

import android.app.PendingIntent
import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import id.icantartil.app.MainActivity

/**
 * Menjaga pemutaran audio qari tetap berjalan saat aplikasi ditutup atau layar mati (layanan latar depan dengan
 * notifikasi media). Pemutarnya tetap AudioEngine.player; layanan ini hanya membungkusnya dengan MediaSession.
 */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        session = MediaSession.Builder(this, AudioEngine.player).setSessionActivity(open).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    /** Aplikasi digeser dari daftar terbaru: lanjut memutar jika sedang berjalan, berhenti jika tidak. */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = AudioEngine.player
        if (!p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onDestroy() {
        session?.release()
        session = null
        super.onDestroy()
    }
}
