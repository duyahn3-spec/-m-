package com.gesture.assist;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
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

        // Phát nhạc (giữ nguyên code cũ)
        try {
            mediaPlayer = MediaPlayer.create(this, R.raw.eclipse_remix);
            if (mediaPlayer != null) {
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            }
        } catch (Exception e) {
            // Bỏ qua nếu không có file nhạc
        }

        // Xin quyền overlay (giữ nguyên)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }

        // Mở cài đặt trợ năng (giữ nguyên)
        Intent accessibilityIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(accessibilityIntent);

        // Nút bấm: bật chức năng kéo giãn + khuếch đại
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Bật chức năng
                Intent cmdIntent = new Intent("com.gesture.assist.TOGGLE_ALL");
                cmdIntent.putExtra("enable", true);
                sendBroadcast(cmdIntent);

                Toast.makeText(GestureAssistActivity.this,
                        "✅ Đã bật kéo giãn 1.7x + khuếch đại cử chỉ",
                        Toast.LENGTH_LONG).show();
                statusText.setText("🟢 TRẠNG THÁI: ĐANG KHUẾCH ĐẠI");
                statusText.setTextColor(0xFF00E676);
                toggleButton.setText("✅ ĐÃ BẬT");

                // Mở trợ năng để người dùng bật thủ công (nếu chưa)
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                startActivity(intent);

                finish();
            }
        });

        // Cập nhật giao diện theo trạng thái trợ năng (giữ nguyên)
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
            toggleButton.setText("⚡ BẬT KHUẾCH ĐẠI + KÉO GIÃN");
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
