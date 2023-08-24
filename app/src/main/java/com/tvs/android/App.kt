package com.tvs.android

import android.app.Application
import com.tvs.VitalsScannerSDK

class App: Application() {

    override fun onCreate() {
        super.onCreate()

        VitalsScannerSDK.init(
            context = this,
            licenseKey = "",
            deviceProvider = AndroidProvider(prefs = getSharedPreferences(PREFS_KEY, MODE_PRIVATE)),
        )
    }
    companion object { const val PREFS_KEY = "shared_preferences_key" }
}
