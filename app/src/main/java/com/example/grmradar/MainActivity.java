package com.example.grmradar;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityManager;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.topjohnwu.superuser.Shell;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private final String SERVICE_NAME = "com.example.grmradar/com.example.grmradar.RadarService";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Shell.getShell(); // Init root

        prefs = getSharedPreferences("RadarPrefs", MODE_PRIVATE);
        Switch switchRadar = findViewById(R.id.switchRadar);
        
        boolean isLayananNyala = isAccessibilityServiceEnabled();
        switchRadar.setChecked(isLayananNyala);
        prefs.edit().putBoolean("isRadarActive", isLayananNyala).apply();

        switchRadar.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // 1. CEK IZIN OVERLAY (LAYAR MERAH) DULU
                if (!Settings.canDrawOverlays(this)) {
                    Toast.makeText(this, "Wajib izinkan 'Tampil di Atas Aplikasi Lain' agar pop-up merah bisa muncul!", Toast.LENGTH_LONG).show();
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                    buttonView.setChecked(false);
                    return;
                }

                // 2. JIKA OVERLAY AMAN, LANJUT CEK AKSESIBILITAS ROOT
                if (!isAccessibilityServiceEnabled()) {
                    if (Shell.isAppGrantedRoot()) {
                        Toast.makeText(this, "Akses Root Ditemukan! Mengaktifkan Radar...", Toast.LENGTH_SHORT).show();
                        aktifkanAksesibilitasViaRoot(true);
                        
                        buttonView.postDelayed(() -> {
                            if (isAccessibilityServiceEnabled()) {
                                simpanStatus(true);
                                Toast.makeText(this, "Radar AKTIF via Root!", Toast.LENGTH_SHORT).show();
                            } else {
                                lemparKePengaturanBiasa(switchRadar);
                            }
                        }, 1000);
                    } else {
                        lemparKePengaturanBiasa(switchRadar);
                    }
                } else {
                    simpanStatus(true);
                }
            } else {
                if (Shell.isAppGrantedRoot()) {
                    aktifkanAksesibilitasViaRoot(false);
                }
                simpanStatus(false);
                Toast.makeText(this, "Radar DIMATIKAN", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnLihatRiwayat).setOnClickListener(v -> startActivity(new Intent(MainActivity.this, RiwayatActivity.class)));
    }

    private void lemparKePengaturanBiasa(Switch switchRadar) {
        Toast.makeText(this, "Mohon izinkan Aksesibilitas GRM-Radar!", Toast.LENGTH_LONG).show();
        startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        switchRadar.setChecked(false);
    }

    private void simpanStatus(boolean status) {
        prefs.edit().putBoolean("isRadarActive", status).apply();
    }

    private void aktifkanAksesibilitasViaRoot(boolean aktifkan) {
        if (aktifkan) {
            String layananLama = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            String layananBaru = (layananLama == null || layananLama.isEmpty()) ? SERVICE_NAME : (layananLama.contains(SERVICE_NAME) ? layananLama : layananLama + ":" + SERVICE_NAME);
            Shell.cmd("settings put secure enabled_accessibility_services " + layananBaru).exec();
            Shell.cmd("settings put secure accessibility_enabled 1").exec();
        } else {
            String layananLama = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (layananLama != null && layananLama.contains(SERVICE_NAME)) {
                String layananBaru = layananLama.replace(":" + SERVICE_NAME, "").replace(SERVICE_NAME + ":", "").replace(SERVICE_NAME, "");
                Shell.cmd("settings put secure enabled_accessibility_services " + layananBaru).exec();
                if (layananBaru.trim().isEmpty()) {
                    Shell.cmd("settings put secure accessibility_enabled 0").exec();
                }
            }
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return false;
        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityEvent.TYPES_ALL_MASK);
        for (AccessibilityServiceInfo service : enabledServices) {
            if (service.getId().contains(getPackageName())) return true;
        }
        return false;
    }
}