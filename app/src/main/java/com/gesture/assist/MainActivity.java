package com.gesture.assist;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private TextView tvStatus;
    private Button btnActivate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Toast.makeText(this, "Bước 1: setContentView", Toast.LENGTH_SHORT).show();
            setContentView(R.layout.activity_main);
            Toast.makeText(this, "Bước 2: setContentView OK", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "LỖI LAYOUT: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        try {
            tvStatus = findViewById(R.id.tvStatus);
            btnActivate = findViewById(R.id.btnActivate);
            Toast.makeText(this, "Bước 3: findViewById OK", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "LỖI FINDVIEW: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }

        btnActivate.setOnClickListener(v -> {
            Toast.makeText(this, "Bước 4: Bấm nút", Toast.LENGTH_SHORT).show();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "🔥 Đã mở cài đặt!", Toast.LENGTH_LONG).show();
            tvStatus.setText("🟢 Đã bật");
            tvStatus.setTextColor(0xFF00E676);
        });

        Toast.makeText(this, "Bước 5: Khởi tạo thành công!", Toast.LENGTH_LONG).show();
    }
}
