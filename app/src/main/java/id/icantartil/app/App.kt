package id.icantartil.app

import android.app.Application
import androidx.work.*
import id.icantartil.app.ads.Ads
import id.icantartil.app.audio.AudioEngine
import id.icantartil.app.data.Prefs
import id.icantartil.app.sync.SyncWorker
import java.util.concurrent.TimeUnit

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        AudioEngine.init(this)
        Ads.init(this)
        // Sinkronisasi harian (batas aturan Quran Foundation: maksimal 7 hari).
        val req = PeriodicWorkRequestBuilder<SyncWorker>(1, TimeUnit.DAYS)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("qf-sync", ExistingPeriodicWorkPolicy.KEEP, req)
    }
}
