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
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private SeekBar seekBar;
    private TextView tvLabel;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("gamepad_settings", MODE_PRIVATE);

        Button btnActivate = findViewById(R.id.btnActivate);
        seekBar = findViewById(R.id.sensitivitySeekBar);
        tvLabel = findViewById(R.id.tvSensitivityLabel);

        int savedSensitivity = prefs.getInt("sensitivity", 20000);
        seekBar.setProgress(savedSensitivity);
        tvLabel.setText("Độ nhạy (hiện tại: " + savedSensitivity + ")");

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvLabel.setText("Độ nhạy (hiện tại: " + progress + ")");
                prefs.edit().putInt("sensitivity", progress).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnActivate.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
            Toast.makeText(this, "🤕 Địt Cụ làm quả lọ đê 💦!", Toast.LENGTH_LONG).show();
        });
    }
}
