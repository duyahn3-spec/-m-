package com.gesture.assist;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class GestureAssistActivity extends Activity {
    private Button toggleButton;
    private TextView statusText;
    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();
    private boolean serviceStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toggleButton = findViewById(R.id.toggleButton);
        statusText = findViewById(R.id.statusText);

        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.eclipse_remix);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            }
        } catch (Exception e) {}

        // Kiểm tra quyền overlay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }

        // Mở cài đặt trợ năng
        Intent accessibilityIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(accessibilityIntent);

        // Nút bấm – chỉ start service khi người dùng bấm và đã có quyền
        toggleButton.setOnClickListener(v -> {
            if (Settings.canDrawOverlays(this) && isAccessibilityEnabled()) {
                startServices();
                Toast.makeText(this, "🔥 Đã bật khuếch đại + tối ưu", Toast.LENGTH_SHORT).show();
                statusText.setText("🟢 ĐANG CHẠY");
                statusText.setTextColor(0xFF00E676);
                toggleButton.setText("✅ ĐÃ BẬT");
                finish();
            } else {
                Toast.makeText(this, "⚠️ Vui lòng bật overlay và trợ năng trước!", Toast.LENGTH_LONG).show();
                // Mở lại cài đặt
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
            }
        });

        updateUI();
    }

    private void startServices() {
        if (serviceStarted) return;
        serviceStarted = true;
        // Khởi động service ShizukuInjector
        Intent serviceIntent = new Intent(this, ShizukuInjectorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        // Gửi broadcast bật khuếch đại
        Intent cmdIntent = new Intent("com.gesture.assist.TOGGLE_ALL");
        cmdIntent.putExtra("enable", true);
        sendBroadcast(cmdIntent);
    }

    private boolean isAccessibilityEnabled() {
        String service = getPackageName() + "/.GestureAssistService";
        try {
            String enabled = Settings.Secure.getString(getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            return enabled != null && enabled.contains(service);
        } catch (Exception e) {
            return false;
        }
    }

    private void updateUI() {
        boolean isEnabled = isAccessibilityEnabled();
        if (isEnabled) {
            statusText.setText("🟢 TRẠNG THÁI: ĐANG CHẠY");
            statusText.setTextColor(0xFF00E676);
            toggleButton.setText("✅ ĐÃ BẬT");
            // Nếu đã bật trợ năng thì tự start service
            if (Settings.canDrawOverlays(this) && !serviceStarted) {
                startServices();
            }
        } else {
            statusText.setText("🔴 TRẠNG THÁI: CHƯA BẬT");
            statusText.setTextColor(0xFFFF4444);
            toggleButton.setText("⚡ BẬT KHUẾCH ĐẠI + TỐI ƯU");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateUI();
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            try { mediaPlayer.start(); } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            try { mediaPlayer.pause(); } catch (Exception ignored) {}
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            try {
                mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
            } catch (Exception ignored) {}
        }
    }
}
