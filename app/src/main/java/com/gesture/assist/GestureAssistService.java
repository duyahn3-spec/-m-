package com.gesture.assist;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Process;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.RemoteViews;
import android.widget.Toast;

public class GestureAssistService extends AccessibilityService {
    private static final float SCALE_FACTOR = 100.0f;
    private static final String CHANNEL_ID = "shell_channel";
    private WindowManager wm;
    private OverlayView overlay;
    private boolean isActive = true;
    private Vibrator vibrator;
    private float lastX, lastY;
    private boolean overlayCreated = false;

    private final BroadcastReceiver toggleReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.gesture.assist.TOGGLE_ALL".equals(intent.getAction())) {
                isActive = intent.getBooleanExtra("enable", true);
                Toast.makeText(GestureAssistService.this,
                        isActive ? "🔥 Khuếch đại x" + SCALE_FACTOR : "🧊 Đã tắt",
                        Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        // Tăng độ ưu tiên process
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_DISPLAY);

        // Tạo overlay với retry
        createOverlayWithRetry();

        // Đăng ký receiver
        registerReceiver(toggleReceiver, new IntentFilter("com.gesture.assist.TOGGLE_ALL"));

        // Khởi tạo notification
        startShellNotification();

        // Bật service Shizuku
        Intent serviceIntent = new Intent(this, ShizukuInjectorService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }

        Toast.makeText(this, "🔥 Khuếch đại x100 + Tối ưu toàn hệ thống", Toast.LENGTH_LONG).show();
    }

    private void createOverlayWithRetry() {
        int retries = 5;
        while (retries-- > 0 && !overlayCreated) {
            try {
                createOverlay();
                overlayCreated = true;
                break;
            } catch (Exception e) {
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            }
        }
        if (!overlayCreated) {
            Toast.makeText(this, "⚠️ Overlay không tạo được, kiểm tra quyền hiển thị", Toast.LENGTH_LONG).show();
        }
    }

    private void createOverlay() {
        if (overlay != null) {
            try { wm.removeView(overlay); } catch (Exception ignored) {}
        }
        overlay = new OverlayView(this);
        overlay.setTouchInterceptor(this::processTouch);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= 26 ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP;
        wm.addView(overlay, params);
    }

    private void processTouch(MotionEvent event) {
        if (!isActive) return;

        int action = event.getActionMasked();
        float x = event.getRawX();
        float y = event.getRawY();

        if (action == MotionEvent.ACTION_DOWN) {
            lastX = x;
            lastY = y;
            vibrate(3);
            return;
        }

        if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_UP) {
            float dx = (x - lastX) * SCALE_FACTOR;
            float dy = (y - lastY) * SCALE_FACTOR;
            float boostedX = x + dx;
            float boostedY = y + dy;

            sendSwipe(lastX, lastY, boostedX, boostedY);
            lastX = x;
            lastY = y;
        }
    }

    private void sendSwipe(float x1, float y1, float x2, float y2) {
        Intent intent = new Intent("com.gesture.assist.SWIPE");
        intent.putExtra("x1", x1);
        intent.putExtra("y1", y1);
        intent.putExtra("x2", x2);
        intent.putExtra("y2", y2);
        intent.putExtra("duration", 0);
        sendBroadcast(intent);
    }

    private void startShellNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Shell Commander",
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        RemoteViews views = new RemoteViews(getPackageName(), R.layout.notification_shell);
        views.setTextViewText(R.id.shell_input_hint, "📟 Bấm để mở Shell Commander");

        Intent intent = new Intent(this, ShellActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Duong Chai Shell")
                .setContentText("📟 Bấm để nhập lệnh ADB")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();

        startForeground(1, notification);
    }

    private void vibrate(int ms) {
        if (vibrator == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, 20));
        } else {
            vibrator.vibrate(ms);
        }
    }

    @Override
    public void onDestroy() {
        isActive = false;
        unregisterReceiver(toggleReceiver);
        if (overlay != null) {
            try { wm.removeView(overlay); } catch (Exception ignored) {}
        }
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    private static class OverlayView extends android.view.View {
        private TouchInterceptor interceptor;

        public OverlayView(Context context) {
            super(context);
            setFocusable(false);
        }

        public void setTouchInterceptor(TouchInterceptor interceptor) {
            this.interceptor = interceptor;
        }

        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (interceptor != null && event.getActionMasked() != MotionEvent.ACTION_OUTSIDE) {
                MotionEvent raw = MotionEvent.obtain(event);
                raw.setLocation(event.getRawX(), event.getRawY());
                interceptor.onTouch(raw);
                raw.recycle();
                return false;
            }
            return false;
        }

        interface TouchInterceptor {
            void onTouch(MotionEvent event);
        }
    }
}
