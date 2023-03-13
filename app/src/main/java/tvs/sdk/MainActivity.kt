package tvs.sdk

import android.content.Context
import android.content.Intent
import android.hardware.Camera
import android.os.Bundle
import android.os.PowerManager
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

import com.tvs.model.User
import com.tvs.utils.ProcessStatus
import com.tvs.vitals.VitalSignsProcessor

const val VITALS_PROCESS_DURATION = 35

class MainActivity : AppCompatActivity() {
    private var preview: SurfaceView? = null
    private lateinit var vsp: VitalSignsProcessor

    //Toast
    private var mainToast: Toast? = null

    //ProgressBar
    private var progBar: ProgressBar? = null
    var progP = 0
    var inc = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vital_signs_process)
        //Here we create the SDK processor and pass there a User data
        vsp = VitalSignsProcessor(
            User(
                height = 180.0,
                weight = 73.0,
                age = 38,
                gen = 1
            )
        )

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


    private val previewCallback: Camera.PreviewCallback = object : Camera.PreviewCallback {
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

            when (vsp.processImage(data, width, height)) {
                /*
                 * The intensity of the RED received from the camera is crucial for the SDK.
                 * SDK checks each frame and returns this status ion case red color is at low level
                 */
                ProcessStatus.RED_INTENSITY_NOT_ENOUGH -> {
                    inc = 0
                    progP = inc
                    progBar!!.progress = progP
                }

                /*
                 * This status means something went wrong. We plan to add more details for that status in next versions.
                 * For now you can just ask user to restart the process.
                 */
                ProcessStatus.MEASUREMENT_FAILED -> {
                    inc = 0
                    progP = inc
                    progBar!!.progress = progP
                    mainToast =
                        Toast.makeText(applicationContext, "Measurement Failed", Toast.LENGTH_SHORT)
                    mainToast!!.show()
                }

                /*
                 * This status means everything goes OK and SDK processing data
                 */
                ProcessStatus.IN_PROGRESS -> {
                    //Did some simple corrections here  with 1.15 magic number so that you wait till the progress bar reaches the end.
                    progP = (inc++ / (VITALS_PROCESS_DURATION*1.15)).toInt()
                    progBar!!.progress = progP
                }

                /*
                 * This status means everything goes OK and SDK processing data
                 */
                ProcessStatus.PROCESS_FINISHED -> {
                    val i = Intent(this@MainActivity, VitalSignsResults::class.java)
                    i.putExtra("O2R", vsp.o2.value)
                    i.putExtra("breath", vsp.Breath.value)
                    i.putExtra("bpm", vsp.Beats.value)
                    i.putExtra("SP", vsp.SP.value)
                    i.putExtra("DP", vsp.DP.value)
                    startActivity(i)
                    finish()
                }

                /*
                 * If you get this status it's better to restart the whole process from the beginning.
                 * This means that we didn't have enough frames
                 */
                ProcessStatus.NEED_MORE_IMAGES -> {
                    //if you get this status it's better to restart the whole process from the beginning
                }
            }
        }
    }

    /*
     * This function is called in case application was paused and then resumed back
     */
    override fun onResume() {
        super.onResume()
        wakeLock!!.acquire()
        camera = Camera.open()
        camera!!.setDisplayOrientation(90)
    }


    /*
     * This function is called in case application was paused
     */
    override fun onPause() {
        super.onPause()
        wakeLock!!.release()
        camera!!.setPreviewCallback(null)
        camera!!.stopPreview()
        camera!!.release()
        camera = null
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