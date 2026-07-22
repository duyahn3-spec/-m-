package com.gesture.assist;

import android.app.Activity;
import android.content.Intent;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toggleButton = findViewById(R.id.toggleButton);
        statusText = findViewById(R.id.statusText);

        // Kiểm tra và xin quyền overlay
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }

        // Mở cài đặt trợ năng để bật
        Intent accessibilityIntent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
        startActivity(accessibilityIntent);

        // Nút bấm để bật/tắt thủ công
        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Gửi broadcast bật chức năng (mặc định ON)
                Intent cmdIntent = new Intent("com.gesture.assist.TOGGLE_ALL");
                cmdIntent.putExtra("enable", true);
                sendBroadcast(cmdIntent);
                Toast.makeText(GestureAssistActivity.this,
                        "✅ Đã bật kéo giãn 1.7x + khuếch đại cử chỉ",
                        Toast.LENGTH_SHORT).show();
                statusText.setText("🔥 ĐANG HOẠT ĐỘNG");
                finish();
            }
        });

        // Cập nhật trạng thái
        updateUI();
    }

    private void updateUI() {
        statusText.setText("🔹 BẤM NÚT ĐỂ BẬT CHỨC NĂNG");
        toggleButton.setText("⚡ BẬT KÉO GIÃN + KHUẾCH ĐẠI");
    }
}
