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

5. You can find the full class implementation here https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/main/app/src/main/java/tvs/sdk/MainActivity.kt

#### Get results
On the class above you can see the status ```kotlinProcessStatus.PROCESS_FINISHED```. So once this status is reached you can get values from the library.
You can see it here https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/main/app/src/main/java/tvs/sdk/MainActivity.kt#L107
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

You can see it here https://github.com/RE-DOCTOR-AI/Android-SDK-Documentation/blob/main/app/src/main/java/tvs/sdk/MainActivity.kt#L35
In case you have imperial measurement system in your apps you can convert that data to metric as we’re doing in our sample apps.
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

##Screenshots
![image](https://user-images.githubusercontent.com/125552714/220102951-12b56f6d-b49a-45a7-abfc-22f493c95a08.png)
![image](https://user-images.githubusercontent.com/125552714/220103087-e00c5662-e045-4707-9d5e-5d4c31811db5.png)
![image](https://user-images.githubusercontent.com/125552714/220103182-2e9f5f2b-06f7-490a-b469-d9b927574c54.png)
![image](https://user-images.githubusercontent.com/125552714/220103263-adea0bd2-a40b-4e68-9ada-a8c10aeb5184.png)
![image](https://user-images.githubusercontent.com/125552714/220103314-a5663a32-8685-4ed5-b3fb-5a39615c7da8.png)

