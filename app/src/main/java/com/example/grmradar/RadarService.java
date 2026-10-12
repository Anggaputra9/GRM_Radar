package com.example.grmradar;

import android.accessibilityservice.AccessibilityService;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
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

public class RadarService extends AccessibilityService {
    private WindowManager windowManager;
    private View popupView;
    private boolean isPopupShowing = false;
    
    // BLACKLIST (Tambahan kata kunci yang sering muncul di dalam web)
    private String[] blacklist = {"slot", "gacor", "zeus", "judi", "judol", "maxwin", "88", "rtp"}; 

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Cek status saklar dari aplikasi
        SharedPreferences prefs = getSharedPreferences("RadarPrefs", MODE_PRIVATE);
        if (!prefs.getBoolean("isRadarActive", false)) {
            hilangkanPopup();
            return; 
        }

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        // 1. CEK GOOGLE SEARCH (Jika sedang cari artikel, abaikan)
        AccessibilityNodeInfo urlNode = getUrlNode(rootNode);
        if (urlNode != null && urlNode.getText() != null) {
            String url = urlNode.getText().toString().toLowerCase();
            if (url.contains("google.com/search") || url.contains("q=")) {
                hilangkanPopup();
                return;
            }
        }

        // 2. SAPU JAGAT SELURUH TEKS DI LAYAR SITUS WEB
        if (cekTeksBerbahaya(rootNode)) {
            tampilkanPopup();
        } else {
            hilangkanPopup();
        }
    }

    // FUNGSI REKURSIF PEMINDAI LAYAR KESELURUHAN
    private boolean cekTeksBerbahaya(AccessibilityNodeInfo node) {
        if (node == null) return false;

        if (node.getText() != null) {
            String teks = node.getText().toString().toLowerCase();
            for (String kata : blacklist) {
                if (teks.contains(kata)) {
                    return true; 
                }
            }
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            if (cekTeksBerbahaya(node.getChild(i))) {
                return true;
            }
        }
        return false;
    }

    private void tampilkanPopup() {
        if (isPopupShowing) return;
        
        catatRiwayat();

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
        isPopupShowing = true;
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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hilangkanPopup() {
        if (isPopupShowing && popupView != null) {
            windowManager.removeView(popupView);
            isPopupShowing = false;
        }
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
