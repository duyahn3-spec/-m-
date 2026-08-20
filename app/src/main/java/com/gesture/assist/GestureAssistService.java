package com.gesture.assist;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class GestureAssistService extends Service {

    private WindowManager windowManager;
    private FrameLayout overlayView;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;

    @Override
    public void onCreate() {
        super.onCreate();
        createOverlay();
        startForeground(1, new NotificationCompat.Builder(this, "channel_id")
                .setContentTitle("GestureAssist")
                .setContentText("Visual Assist is running")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .build());
        startScreenCapture();
    }

    private void createOverlay() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        View overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_trackpad, null);
        // Đảm bảo khai báo layout overlay trong res/layout/overlay_trackpad.xml
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        windowManager.addView(overlayView, params);
    }

    private void startScreenCapture() {
        // Khởi tạo MediaProjection để chụp màn hình
        // Đây là phần cần thiết cho Visual Assist
        // (Code mẫu, mày có thể bổ sung logic xử lý frame tại đây)
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        // Cần Intent từ MainActivity để bắt đầu projection
        // Nếu chưa có, mày có thể bổ sung sau
    }

    // ====== ĐÃ VÔ HIỆU HÓA HOÀN TOÀN ======
    // Không còn hàm injectTouch hay performGesture
    // Mọi thao tác can thiệp vào game đều bị loại bỏ
    // ===========================================

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (overlayView != null) {
            windowManager.removeView(overlayView);
        }
        if (virtualDisplay != null) {
            virtualDisplay.release();
        }
        if (mediaProjection != null) {
            mediaProjection.stop();
        }
        if (imageReader != null) {
            imageReader.close();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
