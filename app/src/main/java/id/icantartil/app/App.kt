package id.icantartil.app

import android.app.Application
import id.icantartil.app.ads.Ads
import id.icantartil.app.audio.AudioEngine
import id.icantartil.app.data.Prefs

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        Prefs.init(this)
        AudioEngine.init(this)
        Ads.init(this)
    }
}
