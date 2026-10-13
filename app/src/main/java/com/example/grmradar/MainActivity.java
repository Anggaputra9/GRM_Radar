package com.example.grmradar;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Switch;
import android.widget.Toast;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("RadarPrefs", MODE_PRIVATE);
        Switch switchRadar = findViewById(R.id.switchRadar);
        switchRadar.setChecked(prefs.getBoolean("isRadarActive", false));

        switchRadar.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !isAccessibilityServiceEnabled()) {
                Toast.makeText(this, "Mohon izinkan GRM-Radar di Pengaturan terlebih dahulu!", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);
                switchRadar.setChecked(false); 
            } else {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("isRadarActive", isChecked);
                editor.apply();
                String pesan = isChecked ? "Radar AKTIF!" : "Radar DIMATIKAN";
                Toast.makeText(this, pesan, Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnLihatRiwayat).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RiwayatActivity.class));
        });
    }

    private boolean isAccessibilityServiceEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return false;
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK);
        for (AccessibilityServiceInfo service : enabledServices) {
            if (service.getId().contains(getPackageName())) {
                return true;
            }
        }
        return false;
    }
}