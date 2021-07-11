package com.github.shingyx.connectspeaker

import android.app.Application
import com.github.shingyx.connectspeaker.data.Preferences
import timber.log.Timber

class ConnectSpeakerApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        Preferences.initialize(this)
    }
}
