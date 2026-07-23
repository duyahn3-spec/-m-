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

public class GestureAssistActivity extends Activity {
    private Button toggleButton;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toggleButton = findViewById(R.id.toggleButton);
        statusText = findViewById(R.id.statusText);

        // Xin quyền overlay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }

        // Mở trợ năng
        Intent accessibilityIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(accessibilityIntent);

        // Bật service ngay lập tức
        Intent serviceIntent = new Intent(this, ShizukuInjectorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        toggleButton.setOnClickListener(v -> {
            Intent cmdIntent = new Intent("com.gesture.assist.TOGGLE_ALL");
            cmdIntent.putExtra("enable", true);
            sendBroadcast(cmdIntent);
            Toast.makeText(this, "🔥 Bật khuếch đại 100x + Tối ưu", Toast.LENGTH_LONG).show();
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
    }
}
