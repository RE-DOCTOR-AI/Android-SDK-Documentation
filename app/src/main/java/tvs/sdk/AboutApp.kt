package tvs.sdk

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class AboutApp : AppCompatActivity() {
    private var p = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_app)
        addNextListeners()
    }

    private fun addNextListeners() {
        //Adding version so that we see it on the main screen
        val versionName = BuildConfig.VERSION_NAME
        val versionNameEditText = findViewById<TextView>(R.id.VersionName)
        versionNameEditText.text = versionName

        val startButton = findViewById<Button>(R.id.Start)

        //checking if users granted permissions to camera
        checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE)

        startButton.setOnClickListener { v: View ->
            p = 5
            val i = Intent(v.context, MainActivity::class.java)
            i.putExtra("Page", p)
            startActivity(i)
            finish()
        }
    }

    // Function to check and request permission.
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

    /*
     * This function is called when the user accepts or decline the permission.
     * Request Code is used to check which permission called this function.
     * This request code is provided when the user is prompt for permission.
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(
            requestCode,
            permissions,
            grantResults
        )
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera Permission Granted", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(
                    this,
                    "Camera Permission Denied. App cannot work without camera permission.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
    }
}