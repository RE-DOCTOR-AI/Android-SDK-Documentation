package tvs.sdk

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
import com.tvs.model.UserParameters
import com.tvs.utils.ProcessStatus
import com.tvs.model.ImageFrameConsumerAndroid
import com.tvs.utils.VITALS_PROCESS_DURATION

class MainActivity : AppCompatActivity() {
    private var preview: SurfaceView? = null
    private lateinit var vitalsFrameConsumer: ImageFrameConsumerAndroid
    private lateinit var glucoseFrameConsumer: ImageFrameConsumerAndroid

    //Toast
    private var mainToast: Toast? = null

    //ProgressBar
    private var progBar: ProgressBar? = null
    var progP = 0
    var inc = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vital_signs_process)
        //SDK: Here we create the SDK frame consumers
        vitalsFrameConsumer = ImageFrameConsumerAndroid(900)
        glucoseFrameConsumer = ImageFrameConsumerAndroid(600)

        // XML - Java Connecting
        preview = findViewById(R.id.preview)
        previewHolder = preview!!.holder
        previewHolder!!.addCallback(surfaceCallback)
        previewHolder!!.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        progBar = findViewById(R.id.VSPB)
        progBar!!.progress = 0


        // WakeLock Initialization : Forces the phone to stay On
        val pm: PowerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "tvs:DoNotDimScreen")
    }


    private val previewCallback: Camera.PreviewCallback = object : Camera.PreviewCallback {

        private fun onStartProcessing() {
            //SDK: Here we create consumers of data
            val vitalsFrames = vitalsFrameConsumer.framesData()
            val glucoseFrames = glucoseFrameConsumer.framesData()

            val i = Intent(this@MainActivity, CalculatingResults::class.java)
            i.putExtra("vitalsData", vitalsFrames)
            i.putExtra("glucoseData", glucoseFrames)
            i.putExtra("userParams", UserParameters(
                height = 180.0,
                weight = 72.0,
                age = 39,
                gen = 1
            ))
            startActivity(i)
            finish()
        }

        //Set of functions below whic got called on different statuses of the SDK
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


        /*
         * This is the main functionality where the main functionality happens
         * Once camera is on and video process begins we pass each frame data to the SDK
         * and check each returned status.
         * Once PROCESS_FINISHED is reached, we can collect Vitals from SDK and pass them to the next stage
         */
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
                onStartProcessing()
            } else if (vitalsFrameResult == ProcessStatus.NEED_MORE_IMAGES
                || glucoseFrameResult == ProcessStatus.NEED_MORE_IMAGES
            ) {
                onMoreImages()
            }
        }
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


    override fun onPause() {
        super.onPause()
        wakeLock!!.release()
        camera!!.setPreviewCallback(null)
        camera!!.stopPreview()
        camera!!.release()
        camera = null
    }

    private fun restartConsuming() {
        inc = 0
        progP = inc
        progBar!!.progress = progP
    }

    private val surfaceCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            try {
                camera!!.setPreviewDisplay(previewHolder)
                camera!!.setPreviewCallback(previewCallback)
            } catch (t: Throwable) {
                println(t.stackTrace)
            }
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            val parameters = camera!!.parameters
            parameters.flashMode = Camera.Parameters.FLASH_MODE_TORCH
            val size = getSmallestPreviewSize(width, height, parameters)
            if (size != null) {
                parameters.setPreviewSize(size.width, size.height)
            }
            camera!!.parameters = parameters
            camera!!.startPreview()
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {}
    }

    companion object {
        //Variables Initialization
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
}