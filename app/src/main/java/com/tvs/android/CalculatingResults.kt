package com.tvs.android

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import android.widget.EditText
import com.tvs.VitalsScannerSDK
import com.tvs.model.FramesDataAndroid
import com.tvs.model.VitalsDto
import com.tvs.processor.ProcessingStatus
import com.tvs.processor.GlucoseLevelProcessorAndroid
import com.tvs.processor.VitalSignsProcessorNg
import com.tvs.android.models.VitalsResult
import kotlinx.coroutines.async

/**
 * Activity to calculate and show results of vital signs and glucose
 */
class CalculatingResults : AppCompatActivity() {
    // Setting up variables for calculated results
    private var glucoseLevelMax = 0
    private var glucoseLevelMin = 0
    private lateinit var vitalsResult: VitalsResult

    // Linking fields and variables
    private val bloodOxygenField by lazy { findViewById<TextView>(R.id.O2V) }
    private val heartRateField by lazy { findViewById<TextView>(R.id.HRV) }
    private val respirationRateField by lazy { findViewById<TextView>(R.id.RRV) }
    private val bloodPressureField by lazy { findViewById<TextView>(R.id.BP2V) }
    private val glucoseLevelField by lazy { findViewById<TextView>(R.id.GlucoseValue) }
    private val pulsePressureField by lazy { findViewById<TextView>(R.id.pulsePressure) }
    private val stressField by lazy { findViewById<TextView>(R.id.stress) }
    private val reflectionIndexField by lazy { findViewById<TextView>(R.id.reflectionIndex) }
    private val lasiField by lazy { findViewById<TextView>(R.id.lasi) }
    private val hrvField by lazy { findViewById<TextView>(R.id.hrv) }

    // Manual readings
    private val bloodOxygenManualField by lazy { findViewById<EditText>(R.id.inputRealO2) }
    private val heartRateManualField by lazy { findViewById<EditText>(R.id.inputRealHr) }
    private val respirationRateManualField by lazy { findViewById<EditText>(R.id.inputRealRR) }
    private val bloodPressureManualField by lazy { findViewById<EditText>(R.id.inputRealBp) }
    private val glucoseManualField by lazy { findViewById<EditText>(R.id.inputRealGlu) }
    private val commentField by lazy { findViewById<EditText>(R.id.comment) }

    private val startAgainBtn by lazy { findViewById<Button>(R.id.StartAgain) }
    private val collectDataBtn by lazy { findViewById<Button>(R.id.CollectData) }

    /**
     * Instantiate processors from SDK for vital signs and glucose
     */
    private val vitalsProcessor = VitalSignsProcessorNg()
    private val glucoseLevelProcessor = GlucoseLevelProcessorAndroid()
    private var vitalsProcessResult: ProcessingStatus = ProcessingStatus.NOT_ENOUGH_DATA

    /**
     * On create method to initialize the activity and kick off data processing
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vital_signs_results)
        Log.d(TAG, "CalculatingResults -> onCreate()")
        enableButtons(false)
        enableDataCollectionViews(VitalsScannerSDK.isDataCollectionEnabled())

        lifecycleScope.launch(Dispatchers.Default) {
            processData()
        }

        startAgainBtn.setOnClickListener { v: View ->
            val intent = Intent(v.context, AboutApp::class.java)
            startActivity(intent)
            finish()
        }
    }

    /**
     * Main entry point for starting data processing. It spawns jobs in coroutines and displays
     * results upon completion.
     */
    private suspend fun processData() {
        /**
         * Extract frames data from the intent
         */
        val vitalsFrameData = getVitalsFrameData()
        val glucoseFrameData = getGlucoseFrameData()

        /* Run data processing in coroutine */
        lifecycleScope.launch(Dispatchers.Main) {
            /**
             * Separate job for calculating vitals
             */
            val vitalsJob = async(Dispatchers.Default) {
                vitalsProcessResult = vitalsProcessor.process(vitalsFrameData)
                println("vitalsProcessResult: $vitalsProcessResult")

                if (vitalsProcessResult == ProcessingStatus.FINISHED) {
                    vitalsResult = VitalsResult(
                        bloodOxygen = vitalsProcessor.o2.value,
                        heartRate = vitalsProcessor.Beats.value,
                        respirationRate = vitalsProcessor.Breath.value,
                        systolicBloodPressure = vitalsProcessor.SP.value,
                        diastolicBloodPressure = vitalsProcessor.DP.value,
                        pulsePressure = vitalsProcessor.pulsePressure.value,
                        stress = vitalsProcessor.stress.value,
                        reflectionIndex = vitalsProcessor.reflectionIndex.value,
                        lasi = vitalsProcessor.lasi.value,
                        hrv = vitalsProcessor.hrv.value,
                    )
                } else {
                    vitalsResult = VitalsResult(
                        bloodOxygen = 0,
                        heartRate = 0,
                        respirationRate = 0,
                        systolicBloodPressure = 0,
                        diastolicBloodPressure = 0,
                        pulsePressure = 0,
                        stress = 0,
                        reflectionIndex = 0.0,
                        lasi = 0.0,
                        hrv = 0.0,
                    )
                }
            }
            vitalsJob.await() // wait for completion
            showVitals() // show computation results

            /**
             * Start glucose loading animation as it takes some time to calculate the results
             */
            val glucoseAnimationJob = lifecycleScope.launch {
                startGlucoseLoadingAnimation().collect { text ->
                    glucoseLevelField.text = text
                }
            }

            /**
             * Separate job for calculating glucose
             */
            val glucoseJob = async(Dispatchers.Default) {
                val glucoseResult = glucoseLevelProcessor.process(glucoseFrameData)
                println("Finished glucose processing with status: $glucoseResult")

                if (glucoseResult == ProcessingStatus.FINISHED) {
                    // Read glucose values from the processor
                    glucoseLevelMax = glucoseLevelProcessor.getGlucoseMaxValue()
                    glucoseLevelMin = glucoseLevelProcessor.getGlucoseMinValue()
                }
            }
            glucoseJob.await() // wait for completion
            glucoseAnimationJob.cancel() // Stop the animation on completion
            showGlucose() // show computation results

            collectDataWithoutRealMeasurements()

            // Enabled buttons once the processing is done
            withContext(Dispatchers.Main) { enableButtons(true) }
        }
    }

    /**
     * Collect data from the current session and store it in the SDK
     */
    private fun collectData() {
        val isCollected = VitalsScannerSDK.logs.addDataCollectionLog(
            getGlucoseFrameData(), VitalsDto(
                vitalsProcessor.SP.value,
                vitalsProcessor.DP.value,
                vitalsProcessor.Beats.value,
                vitalsProcessor.Breath.value,
                vitalsProcessor.o2.value,
                glucoseLevelProcessor.getGlucoseMinValue(),
                glucoseLevelProcessor.getGlucoseMaxValue(),
            ), getRealVitals(),
            VitalsScannerSDK.user,
            this.commentField.text.toString()
        )

        if (isCollected) {
            collectDataBtn.isEnabled = false
            collectDataBtn.text = "Data collection success."
        }
    }

    private fun collectDataWithoutRealMeasurements() {
        VitalsScannerSDK.logs.addDataCollectionLog(
            getGlucoseFrameData(),
            VitalsDto(
                vitalsProcessor.SP.value,
                vitalsProcessor.DP.value,
                vitalsProcessor.Beats.value,
                vitalsProcessor.Breath.value,
                vitalsProcessor.o2.value,
                glucoseLevelProcessor.getGlucoseMinValue(),
                glucoseLevelProcessor.getGlucoseMaxValue(),
            ),
            VitalsDto.emptyVitals(),
            VitalsScannerSDK.user,
            ""
        )
    }

    /**
     * Read real vitals entered manually into designated input fields
     */
    private fun getRealVitals(): VitalsDto {
        val realGlucose = getIntOrMinusOne(glucoseManualField.text.toString())
        val realPulse = getIntOrMinusOne(heartRateManualField.text.toString())
        val realOxygen = getIntOrMinusOne(bloodOxygenManualField.text.toString())
        val realRespiration = getIntOrMinusOne(respirationRateManualField.text.toString())
        val realBloodPressure = parseBloodPressure(bloodPressureManualField.text.toString())

        return VitalsDto(
            realBloodPressure.first,
            realBloodPressure.second,
            realPulse,
            realRespiration,
            realOxygen,
            realGlucose,
            realGlucose
        )
    }

    private fun getIntOrMinusOne(text: String): Int {
        val trimmed = text.trim()
        return if (trimmed.isEmpty()) -1 else trimmed.toInt()
    }

    /**
     * Parse blood pressure reading into systolic and diastolic components
     */
    private fun parseBloodPressure(reading: String): Pair<Int, Int> {
        val components = reading.split("/")

        return if (components.size == 2) {
            val systolic = getIntOrMinusOne(components[0])
            val diastolic = getIntOrMinusOne(components[1])
            Pair(systolic, diastolic)
        } else {
            Pair(-1, -1) // Default values in case of parsing error
        }
    }


    /**
     * Enable or disable data collection views based on the provided flag
     */
    private fun enableDataCollectionViews(enabled: Boolean) {
        bloodOxygenManualField.visibility = if (enabled) View.VISIBLE else View.GONE
        heartRateManualField.visibility = if (enabled) View.VISIBLE else View.GONE
        respirationRateManualField.visibility = if (enabled) View.VISIBLE else View.GONE
        glucoseManualField.visibility = if (enabled) View.VISIBLE else View.GONE
        collectDataBtn.visibility = if (enabled) View.VISIBLE else View.GONE
        commentField.visibility = if (enabled) View.VISIBLE else View.GONE

        if (enabled) {
            collectDataBtn.setOnClickListener { collectData() }
        }
    }

    /**
     * Enable or disable buttons based on the provided flag
     */
    private fun enableButtons(enabled: Boolean) {
        startAgainBtn.isEnabled = enabled
        collectDataBtn.isEnabled = enabled
    }

    /**
     * Start glucose loading animation due to long processing time
     */
    private fun startGlucoseLoadingAnimation(): Flow<String> = flow {
        val text = getText(R.string.processing)
        var count = 0
        while (true) {
            delay(800)
            val dots = ".".repeat((count++) % 4)
            emit("$text$dots")
        }
    }

    private suspend fun showGlucose() {
        Log.d(TAG, "CalculatingResults -> showGlucose()")
        withContext(Dispatchers.Main) {
            glucoseLevelField.text = "[%d - %d]".format(glucoseLevelMin, glucoseLevelMax)
            glucoseLevelField.setTextColor(resources.getColor(R.color.black, theme))
        }
    }

    private suspend fun showVitals() {
        Log.d(TAG, "CalculatingResults -> showVitals()")
        withContext(Dispatchers.Main) {
            bloodOxygenField.text = "${vitalsResult.bloodOxygen}"
            bloodOxygenField.setTextColor(resources.getColor(R.color.black, theme))

            heartRateField.text =
                if (vitalsResult.heartRate == 0) getString(R.string.not_enough_data) else "${vitalsResult.heartRate}"
            heartRateField.setTextColor(resources.getColor(R.color.black, theme))

            respirationRateField.text =
                if (vitalsResult.respirationRate == 0) getString(R.string.not_enough_data) else "${vitalsResult.respirationRate}"
            respirationRateField.setTextColor(resources.getColor(R.color.black, theme))

            bloodPressureField.text =
                "%d / %d".format(vitalsResult.systolicBloodPressure, vitalsResult.diastolicBloodPressure)
            bloodPressureField.setTextColor(resources.getColor(R.color.black, theme))

            pulsePressureField.text = "${vitalsResult.pulsePressure}"
            pulsePressureField.setTextColor(resources.getColor(R.color.black, theme))

            stressField.text = "${vitalsResult.stress}"
            stressField.setTextColor(resources.getColor(R.color.black, theme))

            reflectionIndexField.text = "${vitalsResult.reflectionIndex}"
            reflectionIndexField.setTextColor(resources.getColor(R.color.black, theme))

            lasiField.text = "${vitalsResult.lasi}"
            lasiField.setTextColor(resources.getColor(R.color.black, theme))

            hrvField.text = "${vitalsResult.hrv}"
            hrvField.setTextColor(resources.getColor(R.color.black, theme))
        }
    }

    private fun getGlucoseFrameData(): FramesDataAndroid {
        Log.d(TAG, "CalculatingResults -> getGlucoseFrameData()")
        val bundle = intent.extras ?: throw IllegalArgumentException("No bundle")

        if (!bundle.containsKey("glucoseData")) {
            throw IllegalArgumentException("No glucoseData in bundle")
        }

        return bundle.getSerializable("glucoseData") as FramesDataAndroid?
            ?: throw IllegalArgumentException("No glucoseData in bundle")
    }

    private fun getVitalsFrameData(): FramesDataAndroid {
        Log.d(TAG, "CalculatingResults -> getVitalsFrameData()")
        val bundle = intent.extras ?: throw IllegalArgumentException("No bundle")

        if (!bundle.containsKey("vitalsData")) {
            throw IllegalArgumentException("No vitalsData in bundle")
        }

        return bundle.getSerializable("vitalsData") as FramesDataAndroid?
            ?: throw IllegalArgumentException("No vitalsData in bundle")
    }
}
private const val TAG = "CalculatingResults"