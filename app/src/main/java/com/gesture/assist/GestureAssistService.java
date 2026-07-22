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

    private static final float SCALE_FACTOR = 30.0f;
    private WindowManager wm;
    private OverlayView overlay;
    private boolean isActive = false;
    private Vibrator vibrator;
    private float lastX, lastY;

    @Override
    public void onCreate() {
        super.onCreate();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        createOverlay();
        isActive = true;
        Toast.makeText(this, "Khuếch đại x" + SCALE_FACTOR, Toast.LENGTH_SHORT).show();
    }

    private void createOverlay() {
        if (overlay != null) return;
        overlay = new OverlayView(this);
        overlay.setTouchInterceptor(this::processTouch);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= 26 ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
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
            vibrate(4);
            sendInject(x, y, MotionEvent.ACTION_DOWN);
            return;
        }

        if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_UP) {
            float dx = (x - lastX) * SCALE_FACTOR;
            float dy = (y - lastY) * SCALE_FACTOR;
            float boostedX = Math.max(0, Math.min(5000, x + dx)); // giới hạn tạm
            float boostedY = Math.max(0, Math.min(5000, y + dy));

            sendInject(boostedX, boostedY, MotionEvent.ACTION_MOVE);
            if (action == MotionEvent.ACTION_UP) {
                sendInject(boostedX, boostedY, MotionEvent.ACTION_UP);
            }

            lastX = x;
            lastY = y;
        }
    }

    private void sendInject(float x, float y, int action) {
        Intent intent = new Intent("com.gesture.assist.INJECT");
        intent.putExtra("x", x);
        intent.putExtra("y", y);
        intent.putExtra("action", action);
        sendBroadcast(intent);
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
            }
            return false;
        }

        interface TouchInterceptor {
            void onTouch(MotionEvent event);
        }
    }
}
