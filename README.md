# Android-SDK-Documentation
## Overview of the SDK functionality
Android SDK functionality allows Android developers to add a Vitals & Glucose measurement functionality into their apps by using RE.DOCTOR Android SDK.
The SDK accepts a few parameters as input. It also requires to have some end user data like: Age, Height, Weight & Gender.
SDK requires at least 30 seconds of camera and flash to be on to capture video which is converted to RGB array on the fly which allows to make calculations of Vitals and Glucose.

## Tutorials
### Installing
1. To install the SDK move the VitalsSDK.aar and ksp.jar files into a lib folder in your project directory: ```<Android app root folder>/libs/```
<img width="354" alt="image" src="https://user-images.githubusercontent.com/125552714/230612751-339f8bf3-f24a-4e75-9538-d1ff6585b8a3.png">
2. Add it as a local dependency into your build.gradle file for your Android app
 
```gradle 
dependencies {
    
    //SDK files
    implementation(fileTree("libs"))
}  
```
<img width="670" alt="image" src="https://user-images.githubusercontent.com/125552714/230612913-ddd48a12-26d7-4d4a-a233-2e4942b8ed55.png">

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
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
    <uses-permission android:name="android.permission.INTERNET" />
```

2. Add the following functions and call them before you start working with camera so that you don't need to manually set up camera permissions for your app. You can see how it's done here https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/main/app/src/main/java/tvs/sdk/AboutApp.kt#L44
```kotlin 
    // Function to check and request permission.
    public void checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);

            // Requesting the permission
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        }
    }   
```

3. In your codebase with android components you should implement class extends AppCompatActivity which opens camera and process images in onPreviewFrame method. The complete code can be found here https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/main/app/src/main/java/tvs/sdk/MainActivity.kt
```kotlin 
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
        val pm: PowerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.FULL_WAKE_LOCK, "tvs:DoNotDimScreen")
    }
    
    ...
}    
      
```

4. In that class you should also implement  ```onPreviewFrame``` function which will allow you to pass data to SDK and get statuses
```
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
```

5. You can find the full class implementation here https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/main/app/src/main/java/tvs/sdk/MainActivity.kt
6. You should implement one class to pass to SDK end user parameters (Age, Height, Weight, etc.). See it here: https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/1.3.0/app/src/main/java/com/tvs/android/AndroidProvider.kt
7. You should implement one class to put license key to SDK. See it here: https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/1.3.0/app/src/main/java/com/tvs/android/App.kt

#### Get results
On the class above you can see the status ```vitalsFrameResult == ProcessStatus.START_CALCULATING
&& glucoseFrameResult == ProcessStatus.START_CALCULATING```. So once this status is reached system start calculating process. In our example app we check it asynchronously 
You can see it here https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/main/app/src/main/java/tvs/sdk/MainActivity.kt#L107
```kotlin
...            
    else if (vitalsFrameResult == ProcessStatus.START_CALCULATING
    && glucoseFrameResult == ProcessStatus.START_CALCULATING
    ) {
        //that is the new status required as Glucose calculation takes time and we need to process is
        onStartProcessing()
    }
...

```      

you can see the function ```onStartProcessing()``` which is used to prepare some data for SDK and move user to the final screen where data got calculated and then showed.

```kotlin
...
private fun onStartProcessing() {
    //SDK required: passing frames data to SDK consumers
    val vitalsFrames = vitalsFrameConsumer.framesData()
    val glucoseFrames = glucoseFrameConsumer.framesData()

    //put frames and user params to the intent so that we can use them on next screen
    val intent = Intent(this@VitalSignsProcess, CalculatingResults::class.java)
    intent.putExtra("vitalsData", vitalsFrames)
    intent.putExtra("glucoseData", glucoseFrames)
    intent.putExtra("userParams", UserParameters(
        height = 180.0,//cm
        weight = 74.0,//kg
        age = 39,//years
        gen = 1//1:male, 2: female
    ))
    startActivity(intent)
    finish()
}
...

```

6. On file CalculatingResults you can see the code which works asynchronously

```kotlin
    ...
        //SDK required: vitals and glucose processors
        //-->
        val glucoseLevelProcessor = GlucoseLevelProcessorAndroid()
        val vitalsProcessor = VitalSignsProcessorNg(this.getUserParameters())
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
        ...
```

#### Keep in mind
##### Metric vs Imperial
Library needs some patient data in a metric system so use kilograms(kg) and centimetres (cm). Here is the list:
1. Height (cm)
2. Weight (kg)
3. Age (years)
4. Gender (1 - Male, 2 - Female). We are sorry to ask you to chose only between those two numbers but calculations are depend on them.

You can see it here https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/main/app/src/main/java/tvs/sdk/MainActivity.kt#L35
In case you have imperial measurement system in your apps you can convert that data to metric as we’re doing in our sample apps.
```kotlin
    intent.putExtra("userParams", UserParameters(
        height = 180.0,//cm
        weight = 74.0,//kg
        age = 39,//years
        gen = 1//1:male, 2: female
    ))
```
##### Process duration
Remember that process of measurement lasts for 45 seconds. You can see the constant ```VITALS_PROCESS_DURATION``` which is stored in the SDK and equals 40 seconds. Which means user have to hold their finder during that time.
##### License key
Bear in mind that license key will be rotated (it will be agreed with separately with your company). 
There are few option to do it. Here are two just for example:
1. Keep it inside the app but closeer to rotation date you will have to update it and also update your app for endusers in order they can still use it
2. Keep it out side of the app so that you can just update the key and leave your app without any updates
##### Internet connection
SDK requires periodical internet connection in order to send logs to the server. It's recommended to use it while connected to interned at least once in a few days.
### Troubleshooting
Debug release of SDK writes some outputs to logs so you can see if there are any issues.
## Point of Contact for Support
In case of any questions, please contact timur@re.doctor
## Version details
Current version is 1.2.0 has a basic functionality to measure vitals & glucose including: 

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








