package com.gesture.assist;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import rikka.shizuku.Shizuku;

public class GestureAssistActivity extends Activity {
    private static final int PERMISSION_REQUEST_CODE = 1000;
    private TextView statusText;
    private Button btnAction;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        statusText = findViewById(R.id.statusText);
        btnAction = findViewById(R.id.toggleButton);

        // Kiểm tra và hiển thị trạng thái
        updateStatus();

        // Xử lý nút bấm
        btnAction.setOnClickListener(v -> {
            if (!isShizukuReady()) {
                requestShizukuPermission();
            } else if (!isAccessibilityEnabled()) {
                openAccessibilitySettings();
            } else if (!canDrawOverlays()) {
                requestOverlayPermission();
            } else {
                executeOptimization();
            }
        });

        // Tự động chạy nếu đã có đủ quyền
        if (isShizukuReady() && isAccessibilityEnabled() && canDrawOverlays()) {
            executeOptimization();
        } else {
            // Hướng dẫn người dùng bật quyền
            Toast.makeText(this, "Vui lòng bật tất cả quyền để tối ưu", Toast.LENGTH_LONG).show();
        }
    }

    private void updateStatus() {
        StringBuilder sb = new StringBuilder();
        sb.append("🔹 Shizuku: ").append(isShizukuReady() ? "✅ Đã sẵn sàng" : "❌ Chưa chạy").append("\n");
        sb.append("🔹 Trợ năng: ").append(isAccessibilityEnabled() ? "✅ Đã bật" : "❌ Chưa bật").append("\n");
        sb.append("🔹 Overlay: ").append(canDrawOverlays() ? "✅ Đã bật" : "❌ Chưa bật").append("\n");
        statusText.setText(sb.toString());
    }

    private boolean isShizukuReady() {
        return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
    }

    private void requestShizukuPermission() {
        if (Shizuku.pingBinder()) {
            Shizuku.requestPermission(PERMISSION_REQUEST_CODE);
            Toast.makeText(this, "📢 Đang xin quyền Shizuku...", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "❌ Shizuku chưa chạy! Hãy mở Shizuku app và Start.", Toast.LENGTH_LONG).show();
        }
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

    private void openAccessibilitySettings() {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(intent);
        Toast.makeText(this, "🔧 Bật Duong Chai Dim OK trong Trợ năng", Toast.LENGTH_LONG).show();
    }

    private boolean canDrawOverlays() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return Settings.canDrawOverlays(this);
        }
        return true;
    }

    private void requestOverlayPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
        Toast.makeText(this, "🖥️ Bật Hiển thị trên ứng dụng khác", Toast.LENGTH_LONG).show();
    }

    private void executeOptimization() {
        // 1. Gửi broadcast
        Intent cmdIntent = new Intent("com.gesture.assist.EXECUTE");
        sendBroadcast(cmdIntent);
        Toast.makeText(this, "🚀 Đã gửi lệnh tối ưu!", Toast.LENGTH_SHORT).show();

        // 2. Hiển thị thông báo tiến trình
        statusText.setText("🔄 Đang tối ưu hệ thống...");

        // 3. Đóng activity sau 3 giây
        handler.postDelayed(() -> {
            Toast.makeText(this, "✅ Hoàn tất! Kiểm tra thông báo để xem kết quả.", Toast.LENGTH_LONG).show();
            finish();
        }, 3000);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
