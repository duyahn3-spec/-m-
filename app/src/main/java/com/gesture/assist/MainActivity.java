package com.gesture.assist;

import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_OVERLAY = 1001;
    private static final int REQUEST_CODE_SCREEN_CAPTURE = 1002;
    private Button btnStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = findViewById(R.id.btnStart);

        // === Ẩn hoàn toàn nút/chức năng liên quan đến aim ===
        // Nếu có Button aim, ẩn nó đi
        Button btnAim = findViewById(R.id.btnAim);
        if (btnAim != null) {
            btnAim.setVisibility(View.GONE);
        }
        // Nếu có Switch aim, ẩn nó đi
        // Switch switchAim = findViewById(R.id.switchAim);
        // if (switchAim != null) {
        //     switchAim.setVisibility(View.GONE);
        // }

        btnStart.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    startActivityForResult(intent, REQUEST_CODE_OVERLAY);
                    return;
                }
            }
            startScreenCapture();
        });
    }

    private void startScreenCapture() {
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        if (projectionManager != null) {
            Intent intent = projectionManager.createScreenCaptureIntent();
            startActivityForResult(intent, REQUEST_CODE_SCREEN_CAPTURE);
        } else {
            Toast.makeText(this, "MediaProjection not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_OVERLAY) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startScreenCapture();
            } else {
                Toast.makeText(this, "Overlay permission required", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == REQUEST_CODE_SCREEN_CAPTURE) {
            if (resultCode == RESULT_OK) {
                // Gửi intent đến service để bắt đầu ghi màn hình
                Intent serviceIntent = new Intent(this, GestureAssistService.class);
                serviceIntent.putExtra("resultCode", resultCode);
                serviceIntent.putExtra("data", data);
                startService(serviceIntent);
                Toast.makeText(this, "Visual Assist started", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Screen capture permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
