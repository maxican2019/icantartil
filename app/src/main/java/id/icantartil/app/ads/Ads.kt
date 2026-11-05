package id.icantartil.app.ads

import android.app.Activity
import android.app.Application
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.icandomore.icantartil.BuildConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Iklan rewarded. Default memakai ID uji Google; ganti lewat secret/variabel build untuk produksi. */
object Ads {
    private lateinit var app: Application
    private var ad: RewardedAd? = null

    fun init(a: Application) {
        app = a
        MobileAds.initialize(a)
        load()
    }

    fun load() {
        RewardedAd.load(app, BuildConfig.ADMOB_REWARDED_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(r: RewardedAd) { ad = r }
                override fun onAdFailedToLoad(e: LoadAdError) { ad = null }
            })
    }

    /** @return true jika pengguna menonton sampai hadiah diberikan. */
    suspend fun show(activity: Activity): Boolean {
        if (ad == null) { load(); withTimeoutOrNull(8_000) { while (ad == null) delay(250) } }
        val r = ad ?: return false
        return suspendCancellableCoroutine { c ->
            var earned = false
            r.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() { ad = null; load(); if (c.isActive) c.resume(earned) }
                override fun onAdFailedToShowFullScreenContent(e: AdError) { ad = null; load(); if (c.isActive) c.resume(false) }
            }
            r.show(activity) { earned = true }
        }
    }
}
