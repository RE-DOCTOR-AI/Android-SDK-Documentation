# Android-SDK-Documentation
## Overview of the SDK functionality
Android SDK functionality allows Android developers to add a Vitals & Glucose measurement functionality into their apps by using RE.DOCTOR Android SDK.
The SDK accepts a few parameters as input. It also requires to have some end user data like: Age, Height, Weight & Gender.
SDK requires at least 30 seconds of camera and flash to be on to capture video which is converted to RGB array on the fly which allows to make calculations of Vitals and Glucose.

## Tutorials
### Installing
1. To install the SDK move the com.tvs.shared-1.0.1.jar file into a lib folder in your project directory: ```<Android app root folder>/libs/```
<img width="373" alt="image" src="https://user-images.githubusercontent.com/125552714/219388288-c1b04fba-29e9-4086-8e7e-ee638547cd8c.png">

2. Add it as a local dependency into your build.gradle file for your Android app
 
```gradle 
dependencies {
    
    api files('libs/tvs.shared-1.0.1.jar')
}  
```
<img width="592" alt="image" src="https://user-images.githubusercontent.com/125552714/219388792-0fbb9d8b-7936-43c5-be92-a5fbabcca220.png">

### Using
#### Prepare data and call SDK functions
Here is an example on how to use it. Please remember that you need to work with Android camera and create a special class/classes where you can prepare data for SDK and receive results from it.
1. Add these configuration to access camera to your AndroidManifest file in resource folder
```XML 
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.flash" />
<uses-feature android:name="android.hardware.camera.autofocus" />
<uses-permission android:name="android.permission.FLASHLIGHT" />
<uses-permission android:name="android.permission.WAKE_LOCK" />  
```

2. Add the following functions and call them before you start working with camera so that you don't need to manually set up camera premissions for your app
```kotlin 
private boolean checkPermission() {
    return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
}

private void requestPermission() {
    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CODE);
}      
```

3. In your codebase with android components you should implement class extends AppCompatActivity which opens camera and process images in onPreviewFrame method.
```kotlin 
package tvs.sdk

import android.content.Context
import android.content.Intent
import android.hardware.Camera
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.PowerManager
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.ProgressBar
import android.widget.Toast
import model.User
import utils.ProcessStatus
import utils.VITALS_PROCESS_DURATION
import vitals.VitalSignsProcessor


class MainActivity : AppCompatActivity() {

  private val previewCallback: Camera.PreviewCallback = object : Camera.PreviewCallback {

        override fun onPreviewFrame(data: ByteArray, cam: Camera) {
        //some code will be here
        }
  }      
}      
```

4. There is one method processImage in ```vitals.*Processor``` files which you should use in your android app. It accepts the byte array with image data, camera size (weight + height) and some user’s anthropometric data. It returns ProcessStatus enum, with 5 statuses:
```
RED_INTENSITY_NOT_ENOUGH("Not good red intensity to process. Should start again"),

MEASUREMENT_FAILED("Measurement Failed. Should Start again"),

IN_PROGRESS("Processing in progress"),

PROCESS_FINISHED("Processing finished"),

NEED_MORE_IMAGES("Need more images to process")
```

```kotlin
package tvs.sdk

//imports block goes here

class MainActivity : AppCompatActivity() {

      private val previewCallback: Camera.PreviewCallback = object : Camera.PreviewCallback {
            override fun onPreviewFrame(data: ByteArray, cam: Camera) {
                  val size = cam.parameters.previewSize ?: throw NullPointerException()
      
                  val width = size.width
                  val height = size.height
      
                  when (vsp.processImage(data, width, height)) {
                      ProcessStatus.RED_INTENSITY_NOT_ENOUGH -> {
                          inc = 0
                          progP = inc
                          progBar!!.progress = progP
                      }
                      ProcessStatus.MEASUREMENT_FAILED -> {
                          inc = 0
                          progP = inc
                          progBar!!.progress = progP
                          mainToast =
                              Toast.makeText(applicationContext, "Measurement Failed", Toast.LENGTH_SHORT)
                          mainToast!!.show()
                      }
                      ProcessStatus.IN_PROGRESS -> {
                          progP = inc++ / VITALS_PROCESS_DURATION
                          progBar!!.progress = progP
                      }
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
                      ProcessStatus.NEED_MORE_IMAGES -> {
                          //need to process more images
                      }
                  }
            }
      }      
}
```

5. Here is the code of the whole class
```kotlin
package tvs.sdk

//imports block goes here


class MainActivity : AppCompatActivity() {
    private var preview: SurfaceView? = null
    private lateinit var vsp: VitalSignsProcessor
    private var patientAge : String = ""
    private var patientHeight : String = ""
    private var patientWeight : String = ""
    private var patientGender : String = ""

    //Toast
    private var mainToast: Toast? = null

    //ProgressBar
    private var progBar: ProgressBar? = null
    var progP = 0
    var inc = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vital_signs_process)
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
        /**
         * {@inheritDoc}
         */
        override fun onPreviewFrame(data: ByteArray, cam: Camera) {
            val size = cam.parameters.previewSize ?: throw NullPointerException()

            val width = size.width
            val height = size.height

            when (vsp.processImage(data, width, height)) {
                ProcessStatus.RED_INTENSITY_NOT_ENOUGH -> {
                    inc = 0
                    progP = inc
                    progBar!!.progress = progP
                }
                ProcessStatus.MEASUREMENT_FAILED -> {
                    inc = 0
                    progP = inc
                    progBar!!.progress = progP
                    mainToast =
                        Toast.makeText(applicationContext, "Measurement Failed", Toast.LENGTH_SHORT)
                    mainToast!!.show()
                }
                ProcessStatus.IN_PROGRESS -> {
                    progP = inc++ / VITALS_PROCESS_DURATION
                    progBar!!.progress = progP
                }
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
                ProcessStatus.NEED_MORE_IMAGES -> {
                    //need to process more images
                }
            }
        }
    }

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
    private val surfaceCallback: SurfaceHolder.Callback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            try {
                camera!!.setPreviewDisplay(previewHolder)
                camera!!.setPreviewCallback(previewCallback)
            } catch (t: Throwable) {
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

```
#### Get results
On the class above you can see the status ```kotlinProcessStatus.PROCESS_FINISHED```. So once this status is reached you can get values from the librarys
```kotlin
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
```                
#### Keep in mind
##### Metric vs Imperial
Library needs some patient data in a metric system so use kilograms(kg) and centimetres (cm). Here is the list:
1. Height (cm)
2. Weight (kg)
3. Age (years)
4. Gender (1 - Male, 2 - Female). We are sorry to ask you to chose only between those two numbers but calculations are depend on them.

You can see it here (line 38-47 in the “main  class MainActivity : AppCompatActivity()” code snippet above). In case you have imperial measurement system in your apps you can convert that data to metric as we’re doing in our sample apps.
```kotlin
vsp = VitalSignsProcessor(
            User(
                height = 180.0, //cm
                weight = 73.0, //kg
                age = 38, //years
                gen = 1

            )
  )
```
##### Process duration
Remember that process of measurement lasts for 30 seconds. You can see the constant ```VITALS_PROCESS_DURATION``` which is stored in the SDK and equals 30 seconds. Which means user have to hold their finder during that time.
### Troubleshooting
Debug release of SDK writes some outputs to logs so you can see if there are any issues.
## Point of Contact for Support
In case of any questions, please contact timur@re.doctor
## Version details
Current version is 1.0.1 has a basic functionality to measure vitals including: 

1. Blood Oxygen
2. Respiration Rate
3. Heart Rate
4. Blood Pressure
