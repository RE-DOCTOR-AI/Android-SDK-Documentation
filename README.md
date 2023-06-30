# Android-SDK-Documentation
## Overview of the SDK functionality
Android SDK functionality allows Android developers to add a Vitals & Glucose measurement functionality into their apps by using RE.DOCTOR Android SDK.
The SDK accepts a few parameters as input. It also requires to have some end user data like: Age, Height, Weight & Gender.
SDK requires at least 40 seconds of camera and flash to be on to capture video which is converted to RGB array on the fly which allows to make calculations of Vitals and Glucose.

## Tutorials
### Installing
1. To install the SDK move the SDK aar file into a lib folder in your project directory: ```<Android app root folder>/libs/```
<img width="373" alt="image" src="https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/assets/125552714/9bb85b69-6af8-44d1-87ab-58587404b05d">

3. Add it as a local dependency into your build.gradle file for your Android app
4. For the sdk v 1.2.0 one more dependency ```de.voize:pytorch-lite-multiplatform:0.5.0``` required to be added
 
```gradle 
dependencies {
    ...
    api files('libs/redoctor.shared-1.2.0.aar')

    //addition for sdk v 1.2.0
    implementation("de.voize:pytorch-lite-multiplatform:0.5.0")
}  
```
5. The full list of dependencies shown below and can be seen in code here: https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/SDK-V2/app/build.gradle
<img width="592" alt="image" src="https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/assets/125552714/54e445da-145c-4aef-aeb8-0fbdcd6a286b">

### Using
You can downlad this repo and request for a demo SDK file so that you can check how the integration is working.
#### Prepare data and call SDK functions
Here is an example on how to use it. Please remember that you need to work with Android camera and create a special class/classes where you can prepare data for SDK and receive results from it.
1. Add these configuration to access camera to your AndroidManifest file in resource folder. You can find it here https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/main/app/src/main/AndroidManifest.xml
```XML 
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" />
<uses-feature android:name="android.hardware.camera.flash" />
<uses-feature android:name="android.hardware.camera.autofocus" />
<uses-permission android:name="android.permission.FLASHLIGHT" />
<uses-permission android:name="android.permission.WAKE_LOCK" />  
```

2. Add the following functions and call them before you start working with camera so that you don't need to manually set up camera permissions for your app. You can see how it's done here https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/main/app/src/main/java/tvs/sdk/AboutApp.kt#L44
```kotlin 
    private fun checkPermission(permission: String, requestCode: Int) {
        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_DENIED
        ) {
            // Requesting the permission
            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
        }
    }     
```

3. In your codebase with android components you should implement class extends AppCompatActivity which opens camera and process images in onPreviewFrame method. The complete code can be found here https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/main/app/src/main/java/tvs/sdk/MainActivity.kt
```kotlin 
package tvs.sdk

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
import com.tvs.model.UserParameters
import com.tvs.utils.ProcessStatus
//import com.tvs.vitals.VitalSignsProcessorNg
import com.tvs.model.ImageFrameConsumerAndroid
import com.tvs.utils.VITALS_PROCESS_DURATION

class MainActivity : AppCompatActivity() {
    private var preview: SurfaceView? = null
    //private lateinit var vsp: VitalSignsProcessorNg
    private lateinit var vitalsFrameConsumer: ImageFrameConsumerAndroid
    private lateinit var glucoseFrameConsumer: ImageFrameConsumerAndroid
    ...
    private val previewCallback: Camera.PreviewCallback = object : Camera.PreviewCallback {


        private fun onStartProcessing() {
            val vitalsFrames = vitalsFrameConsumer.framesData()
            val glucoseFrames = glucoseFrameConsumer.framesData()
            ...
        }
        ...
     } 
     ...
}     
```

4. There are few nethods must be called in order to prepare & collect data and process it. See it here: https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/SDK-V2/app/src/main/java/tvs/sdk/MainActivity.kt#L38C9-L39C62
```kotlin
vitalsFrameConsumer = ImageFrameConsumerAndroid(900)
glucoseFrameConsumer = ImageFrameConsumerAndroid(600)
```
5. You need to create ```previewCallback``` method to start the data preparation, collectoin and processing. See it here: https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/SDK-V2/app/src/main/java/tvs/sdk/MainActivity.kt#L73C1-L73C1
```kotlin
       private val previewCallback: Camera.PreviewCallback = object : Camera.PreviewCallback {

        private fun onStartProcessing() {
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
```
7. There are differrent statuses and you can show to the user differrent messages during the measurement process
8. Once yse get the ```ProcessStatus.START_CALCULATING``` you can show a loader to the user so that they awar that it requires some time to get the results. See it here: https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/SDK-V2/app/src/main/java/tvs/sdk/MainActivity.kt#L152
9. We keep calculation process in a separate class here https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/SDK-V2/app/src/main/java/tvs/sdk/CalculatingResults.kt

#### Get results
Once you've got the status ```ProcessingStatus.FINISHED```, you can get the results of calculations. See ot here: https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/SDK-V2/app/src/main/java/tvs/sdk/CalculatingResults.kt#L37C80-L37C105
```kotlin
     if (glucoseResult === ProcessingStatus.FINISHED && vitalsResult == ProcessingStatus.FINISHED) {
         val i = Intent(this@CalculatingResults, VitalSignsResults::class.java)
         i.putExtra("glucoseMin", glucoseLevelProcessor.getGlucoseMinValue())
         i.putExtra("glucoseMax", glucoseLevelProcessor.getGlucoseMaxValue())
         i.putExtra("O2R", vitalsProcessor.o2.value)
         i.putExtra("breath", vitalsProcessor.Breath.value)
         i.putExtra("bpm", vitalsProcessor.Beats.value)
         i.putExtra("SP", vitalsProcessor.SP.value)
         i.putExtra("DP", vitalsProcessor.DP.value)
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

You can see it here https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/SDK-V2/app/src/main/java/tvs/sdk/MainActivity.kt#L83
```kotlin
   i.putExtra("userParams", UserParameters(
       height = 180.0,
       weight = 72.0,
       age = 39,
       gen = 1
   ))
```
and then they are used here for calculating results: https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/SDK-V2/app/src/main/java/tvs/sdk/CalculatingResults.kt#L28
```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calculating_results)

        val glucoseLevelProcessor = GlucoseLevelProcessorAndroid()
        val vitalsProcessor = VitalSignsProcessorNg(this.getUserParameters())
        ...
    }

```
In case you have imperial measurement system in your apps you can convert that data to metric and then pass to the SDK.

##### Process duration
Remember that process of measurement lasts for 40 seconds. You can see the constant ```VITALS_PROCESS_DURATION``` which is stored in the SDK and equals 40 seconds. Which means user have to hold their finder during that time.
### Troubleshooting
Debug release of SDK writes some outputs to logs so you can see if there are any issues.
## Point of Contact for Support
In case of any questions, please contact timur@re.doctor
## Version details
Current version is 1.2.0 has a functionality to measure the following parameters: 

1. Blood Oxygen
2. Respiration Rate
3. Heart Rate
4. Blood Pressure
5. Blood Glucose

## Screenshots
<p float="left">
<img src="https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/assets/125552714/26ec0961-9230-400c-adc5-5680a9620a80" width=15% height=15%>
<img src="https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/assets/125552714/fde9327d-c475-488e-96c4-3aacd79a8d0d" width=15% height=15%>
<img src="https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/assets/125552714/c05331c6-8149-438e-be65-e647127849dc" width=15% height=15%>
<img src="https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/assets/125552714/e3fc997d-ac7e-4ab3-92dd-a80ea10474aa" width=15% height=15%>
</p>
