package com.tvs.android.models

data class VitalsResult(
    val bloodOxygen: Int = 0,
    val heartRate: Int = 0,
    val respirationRate: Int = 0,
    val systolicBloodPressure: Int = 0,
    val diastolicBloodPressure: Int = 0,

    val pulsePressure: Int = 0,
    val stress: Int = 0,
    val reflectionIndex: Double = 0.0,
    val lasi: Double = 0.0,
    val hrv: Double = 0.0,
) {
    fun getExtraVitalsAsString(): String {
        return """
                pulsePressure=$pulsePressure, 
                stress=$stress, 
                reflectionIndex=$reflectionIndex, 
                lasi=$lasi, 
                hrv=$hrv
        """.trimIndent()
    }
}