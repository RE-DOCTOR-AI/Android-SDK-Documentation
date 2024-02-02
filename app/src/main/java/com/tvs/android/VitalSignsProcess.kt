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
import com.tvs.VitalsScannerSDK


class VitalSignsProcess : AppCompatActivity(), ImageReader.OnImageAvailableListener {
    private lateinit var mainToast: Toast

    private var previewHeight = 0
    private var previewWidth = 0
    private var sensorOrientation = 0

    //SDK required: This to vars are required to pass frames data to SDK
    private lateinit var frameConsumer: DefaultFrameConsumerAndroid

    //ProgressBar
    private lateinit var progBar: ProgressBar
    var progP = 0
    var inc = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vital_signs_process)

        // XML - Java Connecting
        progBar = findViewById(R.id.VSPB)
        progBar!!.progress = 0

        setFragment()
    }

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

    private fun getScreenOrientation(): Int {
        return when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_270 -> 270
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_90 -> 90
            else -> 0
        }
    }

    private fun onResult(result: ConsumptionStatus) {
        when (result) {
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
            ConsumptionStatus.NEED_MORE_IMAGES -> {
                onMoreImages()
            }
            else -> {
                println("Unknown status $result must be one of (${ConsumptionStatus.IN_PROGRESS}, ${ConsumptionStatus.START_CALCULATING}, ${ConsumptionStatus.NEED_MORE_IMAGES}, ${ConsumptionStatus.RED_INTENSITY_NOT_ENOUGH}, ${ConsumptionStatus.MEASUREMENT_FAILED})")
            }
        }
    }

    private fun onStartProcessing() {
        //SDK required: passing frames data to SDK consumers
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

    private fun onLowIntensity() {
        val debugStatus: TextView = findViewById(R.id.DebugStatus)
        // Only the original thread that created a view hierarchy can touch its views
        runOnUiThread {
            debugStatus.text = "Process status: Try to cover the flash with your finger"
        }
        restartConsuming()
    }

    private fun onProgress() {
        val debugStatus: TextView = findViewById(R.id.DebugStatus)
        runOnUiThread {
            debugStatus.text = "Process status: measurement in progress..."
        }
        progP = inc++ / VitalsScannerSDK.VITALS_PROCESS_DURATION
        progBar.progress = progP
    }

    private fun onMoreImages() {
        val debugStatus: TextView = findViewById(R.id.DebugStatus)
        debugStatus.text = "Process status: Nearly there!"
    }

    private fun restartConsuming() {
        inc = 0
        progP = inc
        progBar.progress = progP
        frameConsumer.resetFramesData()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val i = Intent(this@VitalSignsProcess, StartVitalSigns::class.java)
        startActivity(i)
        finish()
    }
}