package com.gesture.assist;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Toast;

public class GestureAssistService extends AccessibilityService {
    private static final float SCALE_FACTOR = 30.0f; // Mày tăng lên 50 nếu muốn nhanh hơn
    private WindowManager wm;
    private OverlayView overlay;
    private boolean isActive = false;
    private Vibrator vibrator;
    private float screenWidth, screenHeight;
    private float lastX, lastY;

    @Override
    public void onCreate() {
        super.onCreate();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        createOverlay();
        Toast.makeText(this, "Kích hoạt khuếch đại x" + SCALE_FACTOR, Toast.LENGTH_SHORT).show();
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
        if (!isActive) return;

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
            // Gửi DOWN
            sendInject(rawX, rawY, MotionEvent.ACTION_DOWN);
            return;
        }

        if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_UP) {
            float deltaX = (rawX - lastX) * SCALE_FACTOR;
            float deltaY = (rawY - lastY) * SCALE_FACTOR;
            float boostedX = Math.max(0, Math.min(screenWidth, rawX + deltaX));
            float boostedY = Math.max(0, Math.min(screenHeight, rawY + deltaY));

            // Gửi MOVE và UP
            sendInject(boostedX, boostedY, MotionEvent.ACTION_MOVE);
            if (action == MotionEvent.ACTION_UP) {
                sendInject(boostedX, boostedY, MotionEvent.ACTION_UP);
            }

            lastX = rawX;
            lastY = rawY;
        }
    }

    private void sendInject(float x, float y, int action) {
        Intent intent = new Intent("com.gesture.assist.INJECT_TOUCH");
        intent.putExtra("x", x);
        intent.putExtra("y", y);
        intent.putExtra("action", action);
        sendBroadcast(intent);
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
