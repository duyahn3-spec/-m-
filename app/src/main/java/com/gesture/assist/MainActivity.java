package com.cuto.shizuku.full;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

public class MainActivity extends Activity {
    private TextView tvShizukuStatus;
    private Button btnToggleService;
    private SeekBar sbSensitivity, sbCursorSize;
    private Spinner spinnerTriggerEdge;
    private boolean isServiceRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvShizukuStatus = findViewById(R.id.tvShizukuStatus);
        btnToggleService = findViewById(R.id.btnToggleService);
        sbSensitivity = findViewById(R.id.sbSensitivity);
        sbCursorSize = findViewById(R.id.sbCursorSize);
        spinnerTriggerEdge = findViewById(R.id.spinnerTriggerEdge);

        // Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.trigger_edges, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTriggerEdge.setAdapter(adapter);

        // SeekBar sensitivity: max 1000
        sbSensitivity.setMax(1000);
        sbSensitivity.setProgress(1000); // mặc định 1000

        // SeekBar cursor size
        sbCursorSize.setMax(80);
        sbCursorSize.setProgress(40);

        // Kiểm tra Shizuku
        checkShizuku();

        // Bật/tắt service
        btnToggleService.setOnClickListener(v -> {
            if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != 0) {
                Toast.makeText(this, "Cần Shizuku!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (isServiceRunning) {
                stopService(new Intent(this, CursorService.class));
                isServiceRunning = false;
                btnToggleService.setText("▶ BẬT DỊCH VỤ");
                Toast.makeText(this, "Đã tắt Cuto", Toast.LENGTH_SHORT).show();
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        !Settings.canDrawOverlays(this)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                    Toast.makeText(this, "Cần bật Overlay!", Toast.LENGTH_SHORT).show();
                    return;
                }
                startService(new Intent(this, CursorService.class));
                isServiceRunning = true;
                btnToggleService.setText("⏹ TẮT DỊCH VỤ");
                Toast.makeText(this, "🔥 Cuto Khủng Bố đã bật!", Toast.LENGTH_SHORT).show();
            }
        });

        // SeekBar: Độ nhạy
        sbSensitivity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isServiceRunning) {
                    float sensitivity = progress; // progress = 0-1000
                    updateCursorService(sensitivity);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // SeekBar: Kích thước con trỏ
        sbCursorSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && isServiceRunning) {
                    int size = 20 + progress;
                    updateCursorService(-1, size);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Spinner: Vùng kích hoạt
        spinnerTriggerEdge.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (isServiceRunning) {
                    String edge = parent.getItemAtPosition(position).toString();
                    updateCursorService(-1, -1, edge);
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void checkShizuku() {
        if (Shizuku.pingBinder()) {
            if (Shizuku.checkSelfPermission() == 0) {
                tvShizukuStatus.setText("🟢 Shizuku: Đã kết nối");
                tvShizukuStatus.setTextColor(0xFF55FF55);
            } else {
                tvShizukuStatus.setText("🟡 Shizuku: Đang xin quyền...");
                Shizuku.requestPermission(1000);
                Shizuku.addRequestPermissionResultListener((requestCode, grantResult) -> {
                    if (requestCode == 1000 && grantResult == 0) {
                        runOnUiThread(() -> {
                            tvShizukuStatus.setText("🟢 Shizuku: Đã kết nối");
                            tvShizukuStatus.setTextColor(0xFF55FF55);
                        });
                    }
                });
            }
        } else {
            tvShizukuStatus.setText("🔴 Shizuku: Chưa kết nối");
        }
    }

    private void updateCursorService(float sensitivity) {
        updateCursorService(sensitivity, -1, null);
    }

    private void updateCursorService(int size) {
        updateCursorService(-1, size, null);
    }

    private void updateCursorService(String edge) {
        updateCursorService(-1, -1, edge);
    }

    private void updateCursorService(float sensitivity, int size, String edge) {
        Intent intent = new Intent("com.cuto.shizuku.full.UPDATE_SETTINGS");
        if (sensitivity > 0) intent.putExtra("sensitivity", sensitivity);
        if (size > 0) intent.putExtra("cursorSize", size);
        if (edge != null) intent.putExtra("triggerEdge", edge);
        sendBroadcast(intent);
    }

    private void runOnUiThread(Runnable action) {
        if (android.os.Looper.myLooper() == android.os.Looper.getMainLooper()) {
            action.run();
        } else {
            new android.os.Handler(android.os.Looper.getMainLooper()).post(action);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkShizuku();
    }
}
