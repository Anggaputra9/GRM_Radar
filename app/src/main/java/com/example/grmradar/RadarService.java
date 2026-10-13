package com.example.grmradar;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Pattern;

public class RadarService extends AccessibilityService {
    private WindowManager windowManager;
    private View popupView;
    private boolean isPopupShowing = false;
    private MediaPlayer mediaPlayer; 

    // Daftar kata haram
    private String[] blacklist = {"slot", "gacor", "zeus", "judi", "judol", "maxwin", "rtp"}; 
    
    // Daftar kata aman (Whitelist) agar tidak salah tangkap iklan NU Online dll
    private String[] whitelist = {"ad slot", "ad-slot", "banner slot", "time slot", "slot waktu"};

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        SharedPreferences prefs = getSharedPreferences("RadarPrefs", MODE_PRIVATE);
        if (!prefs.getBoolean("isRadarActive", false)) {
            hilangkanPopup();
            return; 
        }

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        AccessibilityNodeInfo urlNode = getUrlNode(rootNode);
        if (urlNode != null && urlNode.getText() != null) {
            String url = urlNode.getText().toString().toLowerCase();
            if (url.contains("google.com/search") || url.contains("q=")) {
                hilangkanPopup();
                return;
            }
        }

        if (cekTeksBerbahaya(rootNode)) {
            tampilkanPopup();
        } else {
            hilangkanPopup();
        }
    }

    private boolean cekTeksBerbahaya(AccessibilityNodeInfo node) {
        if (node == null) return false;
        
        // Hanya baca teks yang terlihat (hilangkan pembacaan ID/Class yang bikin error)
        if (node.getText() != null) {
            String teks = node.getText().toString().toLowerCase();
            
            // 1. Cek Whitelist dulu. Kalau ini adalah kata aman (seperti kotak iklan), abaikan!
            boolean isAman = false;
            for (String aman : whitelist) {
                if (teks.contains(aman)) {
                    isAman = true;
                    break;
                }
            }
            
            // 2. Kalau bukan kata aman, baru cek pakai Regex (Pendeteksi Kata Utuh)
            if (!isAman) {
                for (String kata : blacklist) {
                    // \b memastikan kata berdiri sendiri. misal: " slot " kedetek, tapi "ad-slot" lolos.
                    if (Pattern.compile("\\b" + kata + "\\b").matcher(teks).find()) {
                        return true; 
                    }
                }
            }
        }
        
        for (int i = 0; i < node.getChildCount(); i++) {
            if (cekTeksBerbahaya(node.getChild(i))) return true;
        }
        return false;
    }

    private void tampilkanPopup() {
        if (isPopupShowing) return;
        catatRiwayat();
        isPopupShowing = true;

        try {
            if (mediaPlayer == null) {
                mediaPlayer = MediaPlayer.create(this, R.raw.alarm);
                mediaPlayer.setLooping(true);
            }
            if (!mediaPlayer.isPlaying()) {
                mediaPlayer.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        LayoutInflater inflater = LayoutInflater.from(this);
        popupView = inflater.inflate(R.layout.layout_popup, null);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        Button btnKembali = popupView.findViewById(R.id.btnKembali);
        btnKembali.setOnClickListener(v -> {
            hilangkanPopup();
            performGlobalAction(GLOBAL_ACTION_BACK);
        });

        windowManager.addView(popupView, params);
    }

    private void catatRiwayat() {
        SharedPreferences prefs = getSharedPreferences("RiwayatPrefs", MODE_PRIVATE);
        String jsonRiwayat = prefs.getString("data", "[]");
        try {
            JSONArray array = new JSONArray(jsonRiwayat);
            int count = array.length() + 1;
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd MMMM yyyy", new Locale("id", "ID"));
            SimpleDateFormat timeFormat = new SimpleDateFormat("HH.mm");
            Date now = new Date();
            JSONObject obj = new JSONObject();
            obj.put("title", "Terdeteksi " + count);
            obj.put("date", dateFormat.format(now));
            obj.put("time", timeFormat.format(now));
            array.put(obj);
            prefs.edit().putString("data", array.toString()).apply();
        } catch (Exception e) {}
    }

    private void hilangkanPopup() {
        if (!isPopupShowing) return;

        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            mediaPlayer.seekTo(0); 
        }

        if (popupView != null) {
            windowManager.removeView(popupView);
        }
        isPopupShowing = false;
    }

    private AccessibilityNodeInfo getUrlNode(AccessibilityNodeInfo node) {
        if (node == null) return null;
        CharSequence className = node.getClassName();
        String viewId = node.getViewIdResourceName();
        if (className != null && className.toString().equals("android.widget.EditText") && 
            viewId != null && viewId.equals("com.android.chrome:id/url_bar")) {
            return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo result = getUrlNode(node.getChild(i));
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public void onInterrupt() {
        hilangkanPopup();
    }
}