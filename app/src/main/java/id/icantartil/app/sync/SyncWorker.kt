package id.icantartil.app.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(c: Context, p: WorkerParameters) : CoroutineWorker(c, p) {
    override suspend fun doWork(): Result {
        ContentSync.syncIfDue(applicationContext, force = true)
        return Result.success()
    }
}
