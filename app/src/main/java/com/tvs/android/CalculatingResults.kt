package com.tvs.android

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log

import com.tvs.model.FramesDataAndroid
import com.tvs.processor.ProcessingStatus
import com.tvs.processor.GlucoseLevelProcessorAndroid
import com.tvs.processor.VitalSignsProcessorNg

class CalculatingResults : AppCompatActivity() {

    //setting up variables for calculated results
    private var bloodOxygen = 0
    private var heartRate = 0
    private var respirationRate = 0
    private var systolicBloodPressure = 0
    private var diastolicBloodPressure = 0
    private var glucoseLevelMax = 0
    private var glucoseLevelMin = 0

    //linking fields and variables
    private val bloodOxygenField by lazy { findViewById<TextView>(R.id.O2V) }
    private val heartRateField by lazy { findViewById<TextView>(R.id.HRV) }
    private val respirationRateField by lazy { findViewById<TextView>(R.id.RRV) }
    private val bloodPressureField by lazy { findViewById<TextView>(R.id.BP2V) }
    private val glucoseLevelField by lazy { findViewById<TextView>(R.id.GlucoseValue) }
    private val startAgain by lazy { findViewById<Button>(R.id.StartAgain) }

    private var glucoseJob: Job? = null
    private var firstField = false
    private var secondField = false
    private var thirdField = false
    private var forthField = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vital_signs_results)
        Log.d(TAG, "CalculatingResults -> onCreate()")

        blockingButtons(false)

        //SDK required: vitals and glucose processors
        //-->
        val glucoseLevelProcessor = GlucoseLevelProcessorAndroid()
        val vitalsProcessor = VitalSignsProcessorNg()
        //<--

        //flag to keep screen on because glucose calculation can take up to two minutes
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        glucoseJob = lifecycleScope.launch {
            startGlucoseLoadingAnimation().collect { text ->
                glucoseLevelField.text = text
                startVitalsLoadingAnimation(text)
            }
        }


        lifecycleScope.launch(Dispatchers.Default)  {
            //SDK required: Here we wait till glucose is calculated and show results
            //-->
            val glucoseFrameData = getGlucoseFrameData()
            val glucoseResult = glucoseLevelProcessor.process(glucoseFrameData)
            if (glucoseResult == ProcessingStatus.FINISHED) {
                glucoseJob?.cancel()
                glucoseLevelMax = glucoseLevelProcessor.getGlucoseMaxValue()
                glucoseLevelMin = glucoseLevelProcessor.getGlucoseMinValue()
            }
            showGlucose()
            withContext(Dispatchers.Main) { blockingButtons(true) }
            //<--
        }

        lifecycleScope.launch(Dispatchers.Default) {
            //SDK required: here we wait till vitals calculated and show them
            //-->
            val vitalsFrameData = getVitalsFrameData()
            val vitalsResult = vitalsProcessor.process(vitalsFrameData)

            if (vitalsResult == ProcessingStatus.FINISHED) {
                bloodOxygen = vitalsProcessor.o2.value
                heartRate = vitalsProcessor.Beats.value
                respirationRate = vitalsProcessor.Breath.value
                systolicBloodPressure = vitalsProcessor.SP.value
                diastolicBloodPressure = vitalsProcessor.DP.value
            }
            showVitals()
            //<--
        }

        startAgain.setOnClickListener { v: View ->
            val intent = Intent(v.context, AboutApp::class.java)
            startActivity(intent)
            finish()
        }
    }

    //to block button till we wait for results of calculation
    private fun blockingButtons(locked: Boolean) {
        startAgain.isEnabled = locked
    }

    //show some simple animation while glucose is being calculated
    private fun startGlucoseLoadingAnimation(): Flow<String> = flow {
        val text = getText(R.string.processing)
        var count = 0
        while (true) {
            delay(800)
            val dots = ".".repeat((count++) % 4)
            emit("$text$dots")
        }
    }

    //show some simple animation while vitals are being calculated
    private fun startVitalsLoadingAnimation(text: String) {
        if (!firstField) bloodOxygenField.text = text
        if (!secondField) heartRateField.text = text
        if (!thirdField) respirationRateField.text = text
        if (!forthField) bloodPressureField.text = text
    }

    //SDK required: show calculated glucose
    //-->
    private suspend fun showGlucose() {
        Log.d(TAG, "CalculatingResults -> showGlucose()")
        withContext(Dispatchers.Main) {
            glucoseLevelField.text = "%d - %d".format(glucoseLevelMin, glucoseLevelMax)
            glucoseLevelField.setTextColor(resources.getColor(R.color.black, theme))
        }
    }
    //<--

    //SDK required: show calculated vitals
    //-->
    private suspend fun showVitals() {
        firstField = true
        withContext(Dispatchers.Main) {
            bloodOxygenField.text = "$bloodOxygen"
            bloodOxygenField.setTextColor(resources.getColor(R.color.black, theme))
        }
        secondField = true
        withContext(Dispatchers.Main) {
            heartRateField.text =
                if (heartRate == 0) getString(R.string.not_enough_data) else "$heartRate"
            heartRateField.setTextColor(resources.getColor(R.color.black, theme))
        }
        thirdField = true
        withContext(Dispatchers.Main) {
            respirationRateField.text =
                if (respirationRate == 0) getString(R.string.not_enough_data) else "$respirationRate"
            respirationRateField.setTextColor(resources.getColor(R.color.black, theme))
        }
        forthField = true
        withContext(Dispatchers.Main) {
            bloodPressureField.text = "%d / %d".format(systolicBloodPressure, diastolicBloodPressure)
            bloodPressureField.setTextColor(resources.getColor(R.color.black, theme))
        }

    }
    //<--


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