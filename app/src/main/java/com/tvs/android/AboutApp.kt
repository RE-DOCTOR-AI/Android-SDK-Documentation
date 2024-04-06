package com.tvs.android;

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
    var json: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about_app)
        initVariables()
        checkPermission()
        addNextListeners()
    }

    private fun initVariables() {
        val sharedPreferences = getSharedPreferences(App.PREFS_KEY, MODE_PRIVATE)
        json = sharedPreferences.getString(PatientInfoActivity.USER_INFO_KEY, "")
    }

    private fun addNextListeners() {
        val versionName = BuildConfig.VERSION_NAME
        val versionNameEditText = findViewById<TextView>(R.id.VersionName)
        versionNameEditText.text = versionName
        val next = findViewById<Button>(R.id.Next)
        val changeMyData = findViewById<Button>(R.id.ChangeMyData)

        //checking that user has already entered data so that we don't ask it again
        if (json!!.isEmpty()) {
            changeMyData.visibility = View.INVISIBLE
        }
        next.setOnClickListener { v: View ->
            if (json!!.isEmpty()) {
                p = 0
                val i = Intent(v.context, PatientInfoActivity::class.java)
                i.putExtra("Page", p)
                startActivity(i)
            } else {
                //todo switch here between vitals (p=1) and glucose (p=2)
                p = 2
                val i = Intent(v.context, StartVitalSigns::class.java)
                i.putExtra("Page", p)
                startActivity(i)
            }
            finish()
        }
        changeMyData.setOnClickListener { v: View ->
            p = 0
            val i = Intent(v.context, PatientInfoActivity::class.java)
            i.putExtra("Page", p)
            startActivity(i)
            finish()
        }
    }

    // Function to check and request permission.
    private fun checkPermission() {
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

    // This function is called when the user accepts or decline the permission.
    // Request Code is used to check which permission called this function.
    // This request code is provided when the user is prompt for permission.
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera Permission Granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val CAMERA_PERMISSION_CODE = 100
    }
}

