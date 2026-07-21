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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import dev.rikka.shizuku.Shizuku;

public class GestureAssistService extends AccessibilityService {

    private static final String TAG = "GestureService";
    private static final float SCALE_FACTOR = 1440.0f;

    private WindowManager wm;
    private OverlayView overlay;
    private ScheduledExecutorService scheduler;
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
                    inputManagerBinder = Shizuku.getSystemService("input");
                    if (inputManagerBinder != null) {
                        injectMethod = inputManagerBinder.getClass().getMethod(
                                "injectInputEvent", MotionEvent.class, int.class
                        );
                        Toast.makeText(this, "Shizuku + Input OK", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Shizuku init error", e);
                }
            } else {
                Shizuku.requestPermission(1000);
                Toast.makeText(this, "Xin quyền Shizuku...", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Shizuku chưa chạy!", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        random = new SecureRandom();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        initShizuku();
        createOverlay();
        Toast.makeText(this, "Kích hoạt 1440x + Shizuku", Toast.LENGTH_SHORT).show();
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

        float deltaX = (rawX - lastX) * SCALE_FACTOR;
        float deltaY = (rawY - lastY) * SCALE_FACTOR;
        float boostedX = Math.max(0, Math.min(screenWidth, lastX + deltaX));
        float boostedY = Math.max(0, Math.min(screenHeight, lastY + deltaY));

        if (action == MotionEvent.ACTION_DOWN && vibrator != null) {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(4, 20));
            } else {
                vibrator.vibrate(4);
            }
        }

        injectTouch(rawX, rawY, MotionEvent.ACTION_DOWN);
        injectTouch(boostedX, boostedY, MotionEvent.ACTION_MOVE);
        injectTouch(boostedX, boostedY, MotionEvent.ACTION_UP);

        lastX = rawX;
        lastY = rawY;
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
        scheduler.shutdown();
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
