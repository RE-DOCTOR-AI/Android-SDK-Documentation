# Android-SDK-Documentation
## Overview of the SDK functionality
Android SDK functionality allows Android developers to add a Vitals & Glucose measurement functionality into their apps by using RE.DOCTOR Android SDK.
The SDK accepts a few parameters as input. It also requires to have some end user data like: Age, Height, Weight & Gender.
SDK requires at least 45 seconds of camera and flash to be on to capture video which is converted to RGB array on the fly which allows to make calculations of Vitals and Glucose.

## Tutorials
### Installing
1. To install the SDK move the VitalsSDK.aar files into a lib folder in your project directory: ```<Android app root folder>/libs/```
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

### Using
You can download this repo and request for the latest SDK archive so that you can check how the integration is working.

#### License key
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

3. In your Android code you should implement class that extends AppCompatActivity which adds fragment with video feed from the camera in onCreate().
At the same time it must create and instance of `DefaultFrameConsumerAndroid` that is responsible for consuming and processing frames from the camera.
The complete code can be found here https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/main/app/src/main/java/com/tvs/android/VitalSignsProcess.kt
    
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
                frameConsumer = DefaultFrameConsumerAndroid(
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

4. In that class you should also implement  ```onPreviewFrame``` function which will allow you to pass data to SDK and get statuses
```
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
Remember that process of measurement lasts for 45 seconds. You can see the constant ```VITALS_PROCESS_DURATION``` which is stored in the SDK and equals 45 seconds. Which means user have to hold their finder during that time.

##### Internet connection
SDK requires periodical internet connection in order to send logs to the server. It's recommended to use it while connected to interned at least once in a few days.
### Troubleshooting
Debug release of SDK writes some outputs to logs so you can see if there are any issues.
## Point of Contact for Support
In case of any questions, please contact timur@re.doctor
## Version details
Current version is 1.5.0 has a basic functionality to measure vitals & glucose including: 

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








