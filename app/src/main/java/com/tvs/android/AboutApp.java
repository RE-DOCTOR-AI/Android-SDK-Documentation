package com.tvs.android;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class AboutApp extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_CODE = 100;
    private int p;
    private SharedPreferences sharedPreferences;
    //String json;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_app);

        initVariables();
        addNextListeners();
    }

    private void initVariables() {
        sharedPreferences = getSharedPreferences(App.PREFS_KEY, MODE_PRIVATE);
    }

    private void addNextListeners() {
        String versionName = BuildConfig.VERSION_NAME;
        TextView versionNameEditText = this.findViewById(R.id.VersionName);
        versionNameEditText.setText(versionName);

        Button next = this.findViewById(R.id.Next);
        checkPermission(Manifest.permission.CAMERA, CAMERA_PERMISSION_CODE);

        next.setOnClickListener(v -> {
            Intent i = new Intent(v.getContext(), StartVitalSigns.class);
            startActivity(i);
            finish();
        });

    }

    // Function to check and request permission.
    public void checkPermission(String permission, int requestCode) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);

            // Requesting the permission
            ActivityCompat.requestPermissions(this, new String[]{permission}, requestCode);
        }
    }

    // This function is called when the user accepts or decline the permission.
    // Request Code is used to check which permission called this function.
    // This request code is provided when the user is prompt for permission.
    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Camera Permission Granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
