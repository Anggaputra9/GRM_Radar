package com.example.grmradar;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
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

// Import library Root
import com.topjohnwu.superuser.Shell;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private SharedPreferences prefs;
    private final String SERVICE_NAME = "com.example.grmradar/com.example.grmradar.RadarService";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Minta akses Root saat aplikasi baru dibuka (Jika HP di-root)
        Shell.getShell();

        prefs = getSharedPreferences("RadarPrefs", MODE_PRIVATE);
        Switch switchRadar = findViewById(R.id.switchRadar);
        
        // Cek status real-time dari sistem
        boolean isLayananNyala = isAccessibilityServiceEnabled();
        switchRadar.setChecked(isLayananNyala);
        prefs.edit().putBoolean("isRadarActive", isLayananNyala).apply();

        switchRadar.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked && !isAccessibilityServiceEnabled()) {
                // Coba jalur Root dulu
                if (Shell.isAppGrantedRoot()) {
                    Toast.makeText(this, "Akses Root Ditemukan! Mengaktifkan Radar diam-diam...", Toast.LENGTH_SHORT).show();
                    aktifkanAksesibilitasViaRoot(true);
                    
                    // Jeda sebentar untuk menunggu sistem merespons
                    buttonView.postDelayed(() -> {
                        if (isAccessibilityServiceEnabled()) {
                            simpanStatus(true);
                            Toast.makeText(this, "Radar AKTIF via Root!", Toast.LENGTH_SHORT).show();
                        } else {
                            // Kalau gagal (misal Magisk nolak), lempar ke pengaturan biasa
                            lemparKePengaturanBiasa(switchRadar);
                        }
                    }, 1000);
                } else {
                    // Kalau HP gak di-root, lakukan cara normal
                    lemparKePengaturanBiasa(switchRadar);
                }
            } else if (!isChecked) {
                // Matikan radar
                if (Shell.isAppGrantedRoot()) {
                    aktifkanAksesibilitasViaRoot(false);
                }
                simpanStatus(false);
                Toast.makeText(this, "Radar DIMATIKAN", Toast.LENGTH_SHORT).show();
            }
        });

        findViewById(R.id.btnLihatRiwayat).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, RiwayatActivity.class));
        });
    }

    private void lemparKePengaturanBiasa(Switch switchRadar) {
        Toast.makeText(this, "Mohon izinkan GRM-Radar di Pengaturan!", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        switchRadar.setChecked(false);
    }

    private void simpanStatus(boolean status) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("isRadarActive", status);
        editor.apply();
    }

    // ==========================================
    // JURUS DEWA: BYPASS AKSESIBILITAS VIA ROOT
    // ==========================================
    private void aktifkanAksesibilitasViaRoot(boolean aktifkan) {
        if (aktifkan) {
            // Ambil layanan yang sudah aktif sebelumnya biar gak ketimpa
            String layananLama = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            String layananBaru;
            
            if (layananLama == null || layananLama.isEmpty()) {
                layananBaru = SERVICE_NAME;
            } else if (!layananLama.contains(SERVICE_NAME)) {
                layananBaru = layananLama + ":" + SERVICE_NAME;
            } else {
                layananBaru = layananLama; // Sudah ada
            }

            // Tulis paksa ke database Settings sistem (Hanya bisa via Root)
            Shell.cmd("settings put secure enabled_accessibility_services " + layananBaru).exec();
            Shell.cmd("settings put secure accessibility_enabled 1").exec();
        } else {
            // Mematikan layanan
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
            if (service.getId().contains(getPackageName())) {
                return true;
            }
        }
        return false;
    }
}