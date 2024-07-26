package com.tvs.android

import android.app.Application
import com.tvs.VitalsScannerSDK

//SDK required: class to provide SDK with license key and variables
//-->
class App : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initializing ReDoctor SDK
        VitalsScannerSDK
            .withContext(this)
            .withDataCollection() // Enables collection of real and inferred data along with PPG signal
            .withValidation("strict") // Possible values: "strict" and "loose". Sets appropriate validation thresholds.
            .initScanner(
                licenseKey = BuildConfig.ReRoctorLicenseKey, // Pass the license key
                userParametersProvider = AndroidProvider(
                    prefs = getSharedPreferences(
                        PREFS_KEY,
                        MODE_PRIVATE
                    )
                ) // Object that provides user parameters, such as height, weight, age
            )
    }

    companion object {
        const val PREFS_KEY = "shared_preferences_key"
    }
}
//<--
