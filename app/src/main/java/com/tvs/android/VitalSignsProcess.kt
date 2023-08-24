package com.tvs.android

import android.content.Context
import android.content.Intent
import android.hardware.Camera
import android.os.Bundle
import android.os.PowerManager
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.tvs.model.ImageFrameConsumerAndroid
import com.tvs.utils.ProcessStatus
import com.tvs.utils.VITALS_PROCESS_DURATION


class VitalSignsProcess : AppCompatActivity() {

    private var preview: SurfaceView? = null

    //SDK required: This to vars are required to pass frames data to SDK
    private lateinit var vitalsFrameConsumer: ImageFrameConsumerAndroid
    private lateinit var glucoseFrameConsumer: ImageFrameConsumerAndroid

    private var mainToast: Toast? = null

    //ProgressBar
    private var progBar: ProgressBar? = null
    var progP = 0
    var inc = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vital_signs_process)

        //SDK required: Creating frames consumers so that SDK can get frames and process data
        vitalsFrameConsumer = ImageFrameConsumerAndroid(1200)
        glucoseFrameConsumer = ImageFrameConsumerAndroid(600)

        // XML - Java Connecting
        preview = findViewById(R.id.preview)
        previewHolder = preview!!.holder
        previewHolder!!.addCallback(surfaceCallback)
        previewHolder!!.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        progBar = findViewById(R.id.VSPB)
        progBar!!.progress = 0

        // WakeLock Initialization : Forces the phone to stay On
        val pm: PowerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "tvs:DoNotDimScreen")
    }


    //Wakelock + Open device camera + set orientation to 90 degree
    //store system time as a start time for the analyzing process
    //your activity to start interacting with the user.
    override fun onResume() {
        super.onResume()
        wakeLock!!.acquire()
        camera = Camera.open()
        camera!!.setDisplayOrientation(90)
    }

    //call back the frames then release the camera + wakelock and Initialize the camera to null
    //Called as part of the activity lifecycle when an activity is going into the background, but has not (yet) been killed. The counterpart to onResume().
    //When activity B is launched in front of activity A,
    //this callback will be invoked on A. B will not be created until A's onPause() returns, so be sure to not do anything lengthy here.
    override fun onPause() {
        super.onPause()
        wakeLock!!.release()
        camera!!.setPreviewCallback(null)
        camera!!.stopPreview()
        camera!!.release()
        camera = null
    }

    //getting frames data from the camera and start the measuring process
    private val previewCallback: Camera.PreviewCallback = object : Camera.PreviewCallback {

        private fun onStartProcessing() {
            //SDK required: passing frames data to SDK consumers
            val vitalsFrames = vitalsFrameConsumer.framesData()
            val glucoseFrames = glucoseFrameConsumer.framesData()

            //put frames and user params to the intent so that we can use them on next screen
            val intent = Intent(this@VitalSignsProcess, CalculatingResults::class.java)
            intent.putExtra("vitalsData", vitalsFrames)
            intent.putExtra("glucoseData", glucoseFrames)

            startActivity(intent)
            finish()
        }

        //SDK required: set of functions called for different SDK returned statuses
        //-->
        private fun onConsumptionFailure() {
            val debugStatus: TextView = findViewById(R.id.DebugStatus)
            debugStatus.text = "Process status: Let's try one more time!"
            restartConsuming()
            mainToast = Toast.makeText(applicationContext, "Measurement Failed", Toast.LENGTH_SHORT)
            mainToast!!.show()
        }

        private fun onLowIntensity() {
            val debugStatus: TextView = findViewById(R.id.DebugStatus)
            debugStatus.text = "Process status: Try to cover the flash with your finger"
            restartConsuming()
        }

        private fun onProgress() {
            val debugStatus: TextView = findViewById(R.id.DebugStatus)
            debugStatus.text = "Process status: measurement in progress..."
            progP = inc++ / VITALS_PROCESS_DURATION
            progBar!!.progress = progP
        }

        private fun onMoreImages() {
            val debugStatus: TextView = findViewById(R.id.DebugStatus)
            debugStatus.text = "Process status: Nearly there!"
        }
        //<--


        //SDK required: function which runs for each frame captured by camera
        //-->
        override fun onPreviewFrame(data: ByteArray, cam: Camera) {
            val size = cam.parameters.previewSize ?: throw NullPointerException()

            val width = size.width
            val height = size.height

            val vitalsFrameResult = vitalsFrameConsumer.ingest(data, width, height)
            val glucoseFrameResult = glucoseFrameConsumer.ingest(data, width, height)

            if (
                vitalsFrameResult == ProcessStatus.RED_INTENSITY_NOT_ENOUGH
                || glucoseFrameResult == ProcessStatus.RED_INTENSITY_NOT_ENOUGH
            ) {
                onLowIntensity()
            } else if (
                vitalsFrameResult == ProcessStatus.MEASUREMENT_FAILED
                || glucoseFrameResult == ProcessStatus.MEASUREMENT_FAILED
            ) {
                onConsumptionFailure()
            } else if (vitalsFrameResult == ProcessStatus.IN_PROGRESS
                || glucoseFrameResult == ProcessStatus.IN_PROGRESS
            ) {
                onProgress()
            } else if (vitalsFrameResult == ProcessStatus.START_CALCULATING
                && glucoseFrameResult == ProcessStatus.START_CALCULATING
            ) {
                //that is the new status required as Glucose calculation takes time and we need to process is
                onStartProcessing()
            } else if (vitalsFrameResult == ProcessStatus.NEED_MORE_IMAGES
                || glucoseFrameResult == ProcessStatus.NEED_MORE_IMAGES
            ) {
                onMoreImages()
            }
        }
    }
    //<--

    //SDK required: this function is required to set up camera correctly
    //-->
    private val surfaceCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            try {
                camera!!.setPreviewDisplay(previewHolder)
                camera!!.setPreviewCallback(previewCallback)
            } catch (t: Throwable) {
            }
        }


        //SDK required: here we turn on flash and set FPS value 30
        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            val parameters = camera!!.parameters
            parameters.flashMode = Camera.Parameters.FLASH_MODE_TORCH
            parameters.setPreviewFpsRange(30000, 30000)

            val size = getSmallestPreviewSize(width, height, parameters)
            if (size != null) {
                parameters.setPreviewSize(size.width, size.height)
            }
            camera!!.parameters = parameters
            camera!!.startPreview()

        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {}
    }
    //<--

    override fun onBackPressed() {
        super.onBackPressed()
        val i = Intent(this@VitalSignsProcess, StartVitalSigns::class.java)
        startActivity(i)
        finish()
    }

    private fun restartConsuming() {
        inc = 0
        progP = inc
        progBar!!.progress = progP
    }

    //SDK required: this object required to set up camera correctly
    //-->
    companion object {
        private var previewHolder: SurfaceHolder? = null
        private var camera: Camera? = null
        private var wakeLock: PowerManager.WakeLock? = null

        private fun getSmallestPreviewSize(
            width: Int,
            height: Int,
            parameters: Camera.Parameters
        ): Camera.Size? {
            var result: Camera.Size? = null
            for (size in parameters.supportedPreviewSizes) {
                if (size.width <= width && size.height <= height) {
                    if (result == null) {
                        result = size
                    } else {
                        val resultArea = result.width * result.height
                        val newArea = size.width * size.height
                        if (newArea < resultArea) result = size
                    }
                }
            }
            return result
        }
    }
    //<--
}