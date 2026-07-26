package com.gesture.assist;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private SeekBar seekBarSensitivity, seekBarDensity;
    private Switch switchSuperTouch, switchPointerSpeed, switchDispatch;
    private TextView tvStatus, tvTitle;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("gamepad_settings", MODE_PRIVATE);

        try {
            tvTitle = findViewById(R.id.tvTitle);
            tvStatus = findViewById(R.id.tvStatus);
            seekBarSensitivity = findViewById(R.id.seekBarSensitivity);
            seekBarDensity = findViewById(R.id.seekBarDensity);
            switchSuperTouch = findViewById(R.id.switchSuperTouch);
            switchPointerSpeed = findViewById(R.id.switchPointerSpeed);
            switchDispatch = findViewById(R.id.switchDispatch);
            Button btnActivate = findViewById(R.id.btnActivate);

            if (tvTitle != null) {
                tvTitle.setText("CU TO KHỦNG BỐ 🤕");
            }

            if (seekBarSensitivity != null) {
                seekBarSensitivity.setProgress(prefs.getInt("sensitivity", 20000));
            }
            if (seekBarDensity != null) {
                seekBarDensity.setProgress(prefs.getInt("density", 177));
            }
            if (switchSuperTouch != null) {
                switchSuperTouch.setChecked(prefs.getBoolean("super_touch", true));
            }
            if (switchPointerSpeed != null) {
                switchPointerSpeed.setChecked(prefs.getBoolean("pointer_speed", true));
            }
            if (switchDispatch != null) {
                switchDispatch.setChecked(prefs.getBoolean("dispatch", false));
            }

            if (seekBarSensitivity != null) {
                seekBarSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        prefs.edit().putInt("sensitivity", progress).apply();
                        updateStatus();
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                });
            }

            if (seekBarDensity != null) {
                seekBarDensity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                    @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        prefs.edit().putInt("density", progress).apply();
                        updateStatus();
                    }
                    @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                    @Override public void onStopTrackingTouch(SeekBar seekBar) {}
                });
            }

            if (switchSuperTouch != null) {
                switchSuperTouch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    prefs.edit().putBoolean("super_touch", isChecked).apply();
                    updateStatus();
                });
            }
            if (switchPointerSpeed != null) {
                switchPointerSpeed.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    prefs.edit().putBoolean("pointer_speed", isChecked).apply();
                    updateStatus();
                });
            }
            if (switchDispatch != null) {
                switchDispatch.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    prefs.edit().putBoolean("dispatch", isChecked).apply();
                    updateStatus();
                });
            }

            if (btnActivate != null) {
                btnActivate.setOnClickListener(v -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    }
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    startActivity(intent);
                    Toast.makeText(this, "🔥 làm cụ quả lọ đê!", Toast.LENGTH_LONG).show();
                });
            }

            updateStatus();

        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khởi tạo: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void updateStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("🔥 Sukac: ").append(seekBarSensitivity != null ? seekBarSensitivity.getProgress() : 0).append("\n");
        sb.append("📐 Vuốt Cu: ").append(seekBarDensity != null ? seekBarDensity.getProgress() : 0).append("\n");
        sb.append("⚡ Địt Con Mẹ: ").append(switchSuperTouch != null ? (switchSuperTouch.isChecked() ? "Suk" : "Bắn") : "N/A").append("\n");
        sb.append("🚀 Sàm Lon: ").append(switchPointerSpeed != null ? (switchPointerSpeed.isChecked() ? "Lọ" : "Tuôn") : "N/A").append("\n");
        sb.append("🎮 Lọ Lọ Lọ : ").append(switchDispatch != null ? (switchDispatch.isChecked() ? "Sướng" : "chảy 💦") : "N/A");
        if (tvStatus != null) {
            tvStatus.setText(sb.toString());
        }
    }
}
