package com.example.grmradar;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONArray;
import org.json.JSONObject;

public class RiwayatActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_riwayat);

        // Tombol Kembali
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        LinearLayout container = findViewById(R.id.containerRiwayat);
        
        // Membaca Database Lokal
        SharedPreferences prefs = getSharedPreferences("RiwayatPrefs", MODE_PRIVATE);
        String jsonStr = prefs.getString("data", "[]");

        try {
            JSONArray array = new JSONArray(jsonStr);
            LayoutInflater inflater = LayoutInflater.from(this);
            
            // Looping terbalik agar data terbaru muncul di paling atas
            for (int i = array.length() - 1; i >= 0; i--) {
                JSONObject obj = array.getJSONObject(i);
                
                View itemView = inflater.inflate(R.layout.item_riwayat, container, false);
                TextView tvTitle = itemView.findViewById(R.id.tvTitle);
                TextView tvDate = itemView.findViewById(R.id.tvDate);
                TextView tvTime = itemView.findViewById(R.id.tvTime);
                
                tvTitle.setText(obj.getString("title"));
                tvDate.setText(obj.getString("date"));
                tvTime.setText(obj.getString("time"));
                
                container.addView(itemView);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
