package com.gesture.assist;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class GestureAssistActivity extends Activity {
    private Button toggleButton;
    private TextView statusText;
    private MediaPlayer mediaPlayer;

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

        // === AUTO BẬT NGAY KHI CÀI APP ===
        // 1. Bật overlay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }

        // 2. Bật trợ năng
        Intent accessibilityIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(accessibilityIntent);

        // 3. Bật khuếch đại + tối ưu
        Intent cmdIntent = new Intent("com.gesture.assist.TOGGLE_ALL");
        cmdIntent.putExtra("enable", true);
        sendBroadcast(cmdIntent);

        // 4. Khởi động service tối ưu ngầm
        Intent serviceIntent = new Intent(this, ShizukuInjectorService.class);
        startForegroundService(serviceIntent);

        Toast.makeText(this, "🔥 Auto bật khuếch đại 100x + Tối ưu", Toast.LENGTH_LONG).show();

        // Cập nhật UI
        toggleButton.setOnClickListener(v -> {
            Intent cmdIntent2 = new Intent("com.gesture.assist.TOGGLE_ALL");
            cmdIntent2.putExtra("enable", true);
            sendBroadcast(cmdIntent2);
            Toast.makeText(this, "🔥 Bật khuếch đại 100x + Shell + Tối ưu", Toast.LENGTH_LONG).show();
            statusText.setText("🟢 ĐANG KHUẾCH ĐẠI & TỐI ƯU");
            statusText.setTextColor(0xFF00E676);
            toggleButton.setText("✅ ĐÃ BẬT");
            finish();
        });

        updateUI();
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
