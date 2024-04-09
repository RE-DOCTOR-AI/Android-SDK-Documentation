# Android-SDK-Documentation
## Overview of the SDK functionality
Android SDK functionality allows Android developers to add a Vitals & Glucose measurement functionality into their apps by using RE.DOCTOR Android SDK.
The SDK accepts a few parameters as input. It also requires to have some end user data like: Age, Height, Weight & Gender.
SDK requires at least 45 seconds of camera and flash to be on to capture video which is converted to RGB array on the fly which allows to make calculations of Vitals and Glucose.

## Tutorials

### Running this demo application

1. Clone this repository
2. Get the latest SDK archive and license key from the RE.DOCTOR team
3. Place the .aar archive in the `app/libs` folder of the project
4. Add the license key to the `<root>/local.properties` file under the key ```tvs.sdk.key=<your.license.key>```
5. Run ./gradlew build to build the project

### Integrating SDK to your application

#### Installing SDK
1. To install the SDK move the VitalsSDK.aar archive into a lib folder in your project directory: `<root>/app/libs/`
<img width="354" alt="image" src="https://user-images.githubusercontent.com/125552714/230612751-339f8bf3-f24a-4e75-9538-d1ff6585b8a3.png">
2. Add it as a local dependency into your build.gradle file for your Android app
 
```gradle 
dependencies {
    /** Other depdencies */
    
    // SDK files
    implementation(fileTree("libs")) // include all files from libs folder
    implementation("org.bitbucket.b_c:jose4j:0.7.8") // transient dependency that is not packaged in aar package
}  
```
<img width="670" alt="image" src="https://user-images.githubusercontent.com/125552714/230612913-ddd48a12-26d7-4d4a-a233-2e4942b8ed55.png">


#### License key
Request a license key from the RE.DOCTOR team.
Bear in mind that license key has expiration date (it will be agreed on separately with your company).
There are few option to do it. Here are two just for example:
1. Keep it inside the app, but before the license expiration date you will have to update your application on the devices to keep functionality working.
2. Keep it outside of your app and request it from your server. This way you can update the key without updating the app.

This demo app is using the first approach and is configured to read license key from the properties file using key `tvs.sdk.key` and store it in the generated BuildConfig class.
When building this app, put your license key in local.properties file
```
tvs.sdk.key=xyz
```

#### Initializing SDK
In your Android app you should initialize the SDK with the following code to start using its functionality:
```kotlin
import com.tvs.VitalsScannerSDK

VitalsScannerSDK
    .withContext(this)
    .withDataCollection() // Enables collection of real and inferred data along with PPG signal
    .withValidation("loose") // Possible values: "strict" and "loose". Sets appropriate validation thresholds.
    .initScanner(
        licenseKey = BuildConfig.ReRoctorLicenseKey, // Pass the license key
        userParametersProvider = AndroidProvider() // Object that provides user parameters, such as height, weight, age
    )
```
See https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/main/app/src/main/java/com/tvs/android/App.kt for details

Implement `UserParametersProvider` interface to provide user parameters to the SDK. 
This demo app uses local storage to store and load user parameters.
See https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/main/app/src/main/java/com/tvs/android/AndroidProvider.kt for details


#### Prepare data and call SDK functions
Here is an example on how to use it. Please remember that you need to work with Android camera API and create a special class/classes to provide a stream of frames to SDK consumer.
1. Add these configuration to access camera to your AndroidManifest file in resource folder. You can find it here https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/main/app/src/main/AndroidManifest.xml
```XML 
    <uses-permission android:name="android.permission.CAMERA" />

    <uses-feature android:name="android.hardware.camera" />
    <uses-feature android:name="android.hardware.camera.flash" />
    <uses-feature android:name="android.hardware.camera.autofocus" />

    <uses-permission android:name="android.permission.FLASHLIGHT" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.INTERNET" />
```

2. Add the following function and call it before you start working with camera so that you don't need to manually set up camera permissions for your app. You can see how it's done here https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/main/app/src/main/java/com/tvs/android/AboutApp.kt#L66
```kotlin 
    // Function to check and request permission.
    fun checkPermission() {
        val isDenied = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_DENIED
    
        if (isDenied) {
            // Requesting the permission
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_CODE
            )
        }
    }
```

3. In order to consume video stream from the camera and forward it to ReDoctor library for processing,
you should create an activity that extends AppCompatActivity and ImageReader.OnImageAvailableListener. 
The activity should create a fragment with video preview in its onCreate() lifecycle method.
Upon creation of preview fragment create, you need to create an instance of `DefaultFrameConsumerAndroid` that is responsible for consuming and processing frames from the camera. 
The constructor expects the width and height of the camera preview to correctly process the incoming frames.

```kotlin
class VitalSignsProcess : AppCompatActivity(), ImageReader.OnImageAvailableListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ...
        setFragment()
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
                frameConsumer = DefaultFrameConsumerAndroid( // instantiating the frame consumer
                    previewWidth,
                    previewHeight
                ) // object for data ingestion
            },
            this,
            R.layout.camera_fragment,
            Size(640, 480)
        )

        camera2Fragment.setCamera(cameraId)
        fragmentManager.beginTransaction().replace(R.id.container, camera2Fragment).commit()
    }
}
```
The complete code can be found here https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/main/app/src/main/java/com/tvs/android/VitalSignsProcess.kt#L25
For the details of camera preview fragment class refer to https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/main/app/src/main/java/com/tvs/android/camera/CameraConnectionFragment.java

4. The same activity should implement  ```onImageAvailable(reader: ImageReader)``` method which will allow you 
to pass each frame of the video stream to SDK consumer.
```kotlin
override fun onImageAvailable(reader: ImageReader) {
    val image = reader.acquireLatestImage() ?: return
    this.onResult(frameConsumer.offer(image, frameNumber))
}
```
See https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/main/app/src/main/java/com/tvs/android/VitalSignsProcess.kt#L49

5. Implement a method `onResult(result: ProcessingResult<ConsumptionStatus>)` to handle the result of the frame consumption.
```kotlin
when (result.value) {
    ConsumptionStatus.VALIDATION_ERROR -> {
        onValidationFailure(result.error!!) // show a message to the user that the data is invalid, restart the process
    }

    ConsumptionStatus.RED_INTENSITY_NOT_ENOUGH -> {
        onLowIntensity() // show a message to the user to move the finger to the center of the camera, restart the process
    }

    ConsumptionStatus.MEASUREMENT_FAILED -> {
        onConsumptionFailure() // general error message, restart the process
    }

    ConsumptionStatus.IN_PROGRESS -> {
        onProgress() // show a message to the user that the measurement is in progress, increment the progress bar, current frame number.
    }

    ConsumptionStatus.START_CALCULATING -> {
        onStartProcessing() // all necessary data has been collected, start processing the data, open the next screen and pass the data to it
    }

    ConsumptionStatus.SKIP -> {
        // do nothing on skip, proceed normally. 
        // Image won't be included in the calculation (first few frames are skipped to allow the user to position the finger correctly)
    }

    else -> {}
}
```
See https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/main/app/src/main/java/com/tvs/android/VitalSignsProcess.kt#L104

#### Get results
In the class above you can see the frame consumption status ```result.value == ConsumptionStatus.START_CALCULATING```. 
Once this status is has been observed, move to result calculation.
See the example https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/main/app/src/main/java/com/tvs/android/VitalSignsProcess.kt#L150

```kotlin
private fun onStartProcessing() {
    // SDK required: passing frames data to SDK consumers
    println("Collected frames. Start processing")
    
    // vitals and glucose calculation use separate lists, so we need to get them from consumer
    val vitalsFrames = frameConsumer.getVitalsFramesData()
    val glucoseFrames = frameConsumer.getGlucoseFrameData()

    // Put frames and to the intent so that we can use them in the next activity
    val intent = Intent(this@VitalSignsProcess, CalculatingResults::class.java)
    intent.putExtra("vitalsData", vitalsFrames)
    intent.putExtra("glucoseData", glucoseFrames)

    startActivity(intent)
    finish()
}
```

6. In `CalculatingResults` activity kick off data processing using the passed frames data in `onCreate()` method.
It is important to run the computations asynchronously in a coroutine to avoid blocking the UI thread.
See for details https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/main/app/src/main/java/com/tvs/android/CalculatingResults.kt#L78

```kotlin

override fun onCreate() {
    lifecycleScope.launch(Dispatchers.Default) {
        processData()
    }
}

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

            // Computation finished successfully
            if (vitalsProcessResult == ProcessingStatus.FINISHED) {
                // Read vitals values from the processor
                heartRate = vitalsProcessor.getHeartRate()
                bloodPressure = vitalsProcessor.getBloodPressure()
                respirationRate = vitalsProcessor.getRespirationRate()
                bloodOxygen = vitalsProcessor.getBloodOxygen()
            } else {
                // process failed computation
            }
        }
        vitalsJob.await() // wait for completion
        showVitals() // show computation results

        /**
         * Separate job for calculating glucose
         */
        val glucoseJob = async(Dispatchers.Default) {
            val glucoseResult = glucoseLevelProcessor.process(glucoseFrameData)

            if (glucoseResult == ProcessingStatus.FINISHED) {
                glucoseAnimationJob.cancel() // Stop the animation on completion
                // Read glucose values from the processor
                glucoseLevelMax = glucoseLevelProcessor.getGlucoseMaxValue()
                glucoseLevelMin = glucoseLevelProcessor.getGlucoseMinValue()
            }
        }
        glucoseJob.await() // wait for completion
        showGlucose() // show computation results
    }
}
```

#### Keep in mind
##### Metric vs Imperial
Library needs some patient data in a metric system so use kilograms(kg) and centimetres (cm). Here is the list:
1. Height (cm)
2. Weight (kg)
3. Age (years)
4. Gender (1 - Male, 2 - Female). We are sorry to ask you to chose only between those two numbers but calculations are depend on them.

You can see it here https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/main/app/src/main/java/com/tvs/android/AndroidProvider.kt#L26
In case you have imperial measurement system in your apps you can convert that data to metric as we’re doing in our sample apps.

##### Process duration
Remember that process of collecting and preprocessing frames lasts for about 30 seconds. 
You can see the constant ```MEASUREMENT_COUNT``` which is stored in the SDK and equals 900 frames.
Given the video frame rate of 30 fps it will take 30 seconds to collect all the frames.

##### Internet connection
SDK requires periodical internet connection in order to send logs to the server. It's recommended to use it while connected to interned at least once in a few days.
### Troubleshooting
Debug release of SDK writes some outputs to logs so you can see if there are any issues.
## Point of Contact for Support
In case of any questions, please contact timur@re.doctor
## Version details
Current version is 1.5.0 has a basic functionality to measure vitals & glucose including and: 

1. Blood Oxygen
2. Respiration Rate
3. Heart Rate
4. Blood Pressure
5. Blood Glucose


## Screenshots
<p float="left">
 <img width="150" alt="image" src="https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/assets/125552714/1af59c62-17fa-4a3f-a150-771a879520e5">
 <img width="150" alt="image" src="https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/assets/125552714/dee08783-20cc-447b-b8f5-1bd44b32cb79">
 <img width="150" alt="image" src="https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/assets/125552714/fc00b4d4-0867-407a-9f6d-8320847dc330">
 <img width="150" alt="image" src="https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/assets/125552714/be4da00e-6271-426f-81a0-8e628a4293b0">
 <img width="150" alt="image" src="https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/assets/125552714/075fba55-5bb1-4156-ab94-a8e9ad6b9220">
</p>








