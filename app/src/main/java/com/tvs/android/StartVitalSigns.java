package com.tvs.android;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class StartVitalSigns extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_vital_signs);

        findViewById(R.id.StartVS).setOnClickListener(v -> {
            Intent intent = new Intent(v.getContext(), VitalSignsProcess.class);
            System.out.println("VitalSignsProcess started");
            startActivity(intent);
            finish();

        });
    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(StartVitalSigns.this, AboutApp.class);
        startActivity(intent);
        finish();
        super.onBackPressed();
    }
}
