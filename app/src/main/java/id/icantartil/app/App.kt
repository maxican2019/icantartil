package com.icandomore.icantartil

import android.app.Application
import com.icandomore.icantartil.ads.Ads
import com.icandomore.icantartil.audio.AudioEngine
import com.icandomore.icantartil.audio.Narrator
import com.icandomore.icantartil.data.Prefs

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        AudioEngine.init(this)
        Narrator.init(this)
        Ads.init(this)
    }
}
