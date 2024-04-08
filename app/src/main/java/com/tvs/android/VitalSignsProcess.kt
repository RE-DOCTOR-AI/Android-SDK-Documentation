package com.tvs.android

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Bundle
import android.util.Size
import android.view.Surface
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tvs.android.camera.CameraConnectionFragment
import com.tvs.consumer.DefaultFrameConsumerAndroid
import com.tvs.consumer.ConsumptionStatus
import com.tvs.model.ProcessingResult
import com.tvs.VitalsScannerSDK
import kotlin.math.roundToInt

/**
 * Activity for ingesting frames from camera and passing them to SDK
 * It uses fragment api to include camera preview
 */
class VitalSignsProcess : AppCompatActivity(), ImageReader.OnImageAvailableListener {
    private lateinit var mainToast: Toast

    private var previewHeight = 0
    private var previewWidth = 0
    private var sensorOrientation = 0

    // SDK required: This to vars are required to pass frames data to SDK
    private lateinit var frameConsumer: DefaultFrameConsumerAndroid

    // Progress bar
    private val progressBar by lazy { findViewById<ProgressBar>(R.id.VSPB) }
    var inc = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vital_signs_process)
        progressBar.progress = 0
        setFragment()
    }

    /**
     * Callback for image reader. Reads the latest image and passes it to the frame consumer
     */
    override fun onImageAvailable(reader: ImageReader) {
        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return
        }

        try {
            val image = reader.acquireLatestImage() ?: return
            this.onResult(frameConsumer.offer(image, inc))
        } catch (e: Exception) {
            e.printStackTrace()
            return
        }
    }

    /**
     * Creates camera fragment, adds it to the container and initializes frame consumer with camera preview size
     */
    private fun setFragment() {
        val manager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val cameraId = manager.cameraIdList[0]

        manager.setTorchMode(cameraId, true)

        val camera2Fragment = CameraConnectionFragment.newInstance(
            { size, rotation ->
                previewHeight = size.height
                previewWidth = size.width
                sensorOrientation = rotation - getScreenOrientation()
                frameConsumer = DefaultFrameConsumerAndroid(previewWidth, previewHeight)
            },
            this,
            R.layout.camera_fragment,
            Size(640, 480)
        )

        camera2Fragment.setCamera(cameraId)
        fragmentManager.beginTransaction().replace(R.id.container, camera2Fragment).commit()
    }

    /**
     * Returns screen orientation in degrees
     */
    private fun getScreenOrientation(): Int {
        return when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_270 -> 270
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_90 -> 90
            else -> 0
        }
    }

    /**
     * Processes the result of the frame consumption
     */
    private fun onResult(result: ProcessingResult<ConsumptionStatus>) {
        when (result.value) {
            ConsumptionStatus.VALIDATION_ERROR -> {
                onValidationFailure(result.error!!)
            }

            ConsumptionStatus.RED_INTENSITY_NOT_ENOUGH -> {
                onLowIntensity()
            }

            ConsumptionStatus.MEASUREMENT_FAILED -> {
                onConsumptionFailure()
            }

            ConsumptionStatus.IN_PROGRESS -> {
                onProgress()
            }

            ConsumptionStatus.START_CALCULATING -> {
                onStartProcessing()
            }

            ConsumptionStatus.SKIP -> {
                // do nothing on skip, proceed normally. Image won't be included in the calculation
            }

            else -> {}
        }
    }

    /**
     * Handles the case when validation fails, sets the error message and restarts the consuming
     */
    private fun onValidationFailure(error: String) {
        val debugStatus: TextView = findViewById(R.id.DebugStatus)

        runOnUiThread {
            debugStatus.text = "Validation failed: $error"
        }

        restartConsuming()
    }

    /**
     * Handles the case when the consumption is finished and the processing should start
     */
    private fun onStartProcessing() {
        // SDK required: passing frames data to SDK consumers
        println("Collected frames. Start processing")
        val vitalsFrames = frameConsumer.getVitalsFramesData()
        val glucoseFrames = frameConsumer.getGlucoseFrameData()

        //put frames and user params to the intent so that we can use them on next screen
        val intent = Intent(this@VitalSignsProcess, CalculatingResults::class.java)
        intent.putExtra("vitalsData", vitalsFrames)
        intent.putExtra("glucoseData", glucoseFrames)

        startActivity(intent)
        finish()
    }

    /**
     * Handles the case when the consumption fails for some reason
     */
    private fun onConsumptionFailure() {
        val debugStatus: TextView = findViewById(R.id.DebugStatus)
        runOnUiThread {
            debugStatus.text = "Process status: Let's try one more time!"
        }
        restartConsuming()
        mainToast =
            Toast.makeText(applicationContext, "Measurement Failed", Toast.LENGTH_SHORT)
        mainToast.show()
    }

    /**
     * Handles the case when the intensity of the red light is too low
     */
    private fun onLowIntensity() {
        val debugStatus: TextView = findViewById(R.id.DebugStatus)
        // Only the original thread that created a view hierarchy can touch its views
        runOnUiThread {
            debugStatus.text = "Process status: Try to cover the flash with your finger"
        }
        restartConsuming()
    }

    /**
     * Updates the progress bar according to the progress of the measurement
     */
    private fun onProgress() {
        val debugStatus: TextView = findViewById(R.id.DebugStatus)
        runOnUiThread {
            debugStatus.text = "Process status: measurement in progress..."
        }
        this.inc++
        val progress = 100 * (this.inc.toDouble() / VitalsScannerSDK.MEASUREMENT_COUNT)
        progressBar.progress = progress.roundToInt()
    }

    /**
     * Resets the progress bar and frame consumer
     */
    private fun restartConsuming() {
        inc = 0
        progressBar.progress = 0
        frameConsumer.resetFramesData()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val i = Intent(this@VitalSignsProcess, StartVitalSigns::class.java)
        startActivity(i)
        finish()
    }
}