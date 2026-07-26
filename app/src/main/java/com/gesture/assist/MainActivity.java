package com.gesture.assist;

import android.app.Activity;
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

public class MainActivity extends Activity {
    private SeekBar seekBarSensitivity, seekBarDensity;
    private Switch switchSuperTouch, switchPointerSpeed, switchDispatch;
    private TextView tvStatus, tvTitle;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("gamepad_settings", MODE_PRIVATE);

        // Ánh xạ view
        tvTitle = findViewById(R.id.tvTitle);
        tvStatus = findViewById(R.id.tvStatus);
        seekBarSensitivity = findViewById(R.id.seekBarSensitivity);
        seekBarDensity = findViewById(R.id.seekBarDensity);
        switchSuperTouch = findViewById(R.id.switchSuperTouch);
        switchPointerSpeed = findViewById(R.id.switchPointerSpeed);
        switchDispatch = findViewById(R.id.switchDispatch);
        Button btnActivate = findViewById(R.id.btnActivate);

        // Khôi phục cài đặt
        seekBarSensitivity.setProgress(prefs.getInt("sensitivity", 20000));
        seekBarDensity.setProgress(prefs.getInt("density", 177));
        switchSuperTouch.setChecked(prefs.getBoolean("super_touch", true));
        switchPointerSpeed.setChecked(prefs.getBoolean("pointer_speed", true));
        switchDispatch.setChecked(prefs.getBoolean("dispatch", false));

        // Lưu khi thay đổi
        seekBarSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                prefs.edit().putInt("sensitivity", progress).apply();
                updateStatus();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekBarDensity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                prefs.edit().putInt("density", progress).apply();
                updateStatus();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        switchSuperTouch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("super_touch", isChecked).apply();
            updateStatus();
        });
        switchPointerSpeed.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("pointer_speed", isChecked).apply();
            updateStatus();
        });
        switchDispatch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dispatch", isChecked).apply();
            updateStatus();
        });

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

        updateStatus();
    }

    private void updateStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("🔥 Sukac: ").append(seekBarSensitivity.getProgress()).append("\n");
        sb.append("📐 Vuốt Cu: ").append(seekBarDensity.getProgress()).append("\n");
        sb.append("⚡ Địt Con Mẹ: ").append(switchSuperTouch.isChecked() ? "Suk" : "Bắn").append("\n");
        sb.append("🚀 Sàm Lon: ").append(switchPointerSpeed.isChecked() ? "Lọ" : "Tuôn").append("\n");
        sb.append("🎮 Lọ Lọ Lọ : ").append(switchDispatch.isChecked() ? "Sướng" : "chảy 💦");
        tvStatus.setText(sb.toString());
    }
}
