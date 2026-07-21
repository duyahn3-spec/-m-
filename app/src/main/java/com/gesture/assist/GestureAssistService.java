package com.gesture.assist;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Toast;
import java.lang.reflect.Method;
import java.security.SecureRandom;
import rikka.shizuku.Shizuku;

public class GestureAssistService extends AccessibilityService {

    private static final String TAG = "GestureService";
    private static final float SCALE_FACTOR = 20.0f; // Mày muốn 3-4 thì set 30-40

    private WindowManager wm;
    private OverlayView overlay;
    private boolean isActive = false;
    private Vibrator vibrator;
    private float screenWidth, screenHeight;
    private float lastX, lastY;
    private SecureRandom random;

    private IBinder inputManagerBinder;
    private Method injectMethod;
    private boolean isShizukuReady = false;

    private void initShizuku() {
        if (Shizuku.pingBinder()) {
            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                isShizukuReady = true;
                try {
                    // Lấy service "input" qua Shizuku
                    inputManagerBinder = Shizuku.getSystemService("input");
                    if (inputManagerBinder != null) {
                        // Lấy method injectInputEvent từ InputManager
                        injectMethod = inputManagerBinder.getClass().getMethod(
                                "injectInputEvent", MotionEvent.class, int.class
                        );
                        Toast.makeText(this, "Shizuku + Input OK", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Không lấy được InputManager", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Shizuku init error", e);
                    Toast.makeText(this, "Lỗi Shizuku: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            } else {
                Shizuku.requestPermission(1000);
                Toast.makeText(this, "Đang xin quyền Shizuku...", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Shizuku chưa chạy! Hãy khởi động Shizuku trước.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        random = new SecureRandom();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        initShizuku();
        createOverlay();
        Toast.makeText(this, "Kích hoạt Duong Chai Dim OK", Toast.LENGTH_SHORT).show();
        isActive = true;
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
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP;
        wm.addView(overlay, params);
    }

    private void processTouch(MotionEvent rawEvent) {
        if (!isActive || !isShizukuReady || injectMethod == null) return;

        int action = rawEvent.getActionMasked();
        float rawX = rawEvent.getRawX();
        float rawY = rawEvent.getRawY();

        if (action == MotionEvent.ACTION_DOWN) {
            lastX = rawX;
            lastY = rawY;
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= 26) {
                    vibrator.vibrate(VibrationEffect.createOneShot(4, 20));
                } else {
                    vibrator.vibrate(4);
                }
            }
            return;
        }

        if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_UP) {
            float deltaX = (rawX - lastX) * SCALE_FACTOR;
            float deltaY = (rawY - lastY) * SCALE_FACTOR;
            float boostedX = Math.max(0, Math.min(screenWidth, rawX + deltaX));
            float boostedY = Math.max(0, Math.min(screenHeight, rawY + deltaY));

            // Inject sự kiện DOWN tại vị trí raw
            injectTouch(rawX, rawY, MotionEvent.ACTION_DOWN);
            // Inject sự kiện MOVE với vị trí đã nhân
            injectTouch(boostedX, boostedY, MotionEvent.ACTION_MOVE);
            // Inject sự kiện UP tại vị trí nhân (hoặc giữ nguyên)
            injectTouch(boostedX, boostedY, MotionEvent.ACTION_UP);

            lastX = rawX;
            lastY = rawY;
        }
    }

    private void injectTouch(float x, float y, int action) {
        if (injectMethod == null) return;
        long now = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(now, now, action, x, y, 0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try {
            injectMethod.invoke(inputManagerBinder, event, 0);
        } catch (Exception e) {
            Log.e(TAG, "Inject fail", e);
        } finally {
            event.recycle();
        }
    }

    @Override
    public void onDestroy() {
        isActive = false;
        if (overlay != null) {
            try { wm.removeView(overlay); } catch (Exception ignored) {}
            overlay = null;
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
            }
            return false;
        }

        interface TouchInterceptor {
            void onTouch(MotionEvent event);
        }
    }
}
