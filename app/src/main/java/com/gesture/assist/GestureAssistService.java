package com.gesture.assist;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Toast;

public class GestureAssistService extends AccessibilityService {
    private static final float SCALE_FACTOR = 5.0f;
    private static final float MIN_DELTA = 2.0f;
    private WindowManager wm;
    private OverlayView overlay;
    private boolean isActive = true;
    private Vibrator vibrator;
    private float lastX, lastY;
    private boolean isTouching = false;

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
        createOverlay();
        registerReceiver(toggleReceiver, new IntentFilter("com.gesture.assist.TOGGLE_ALL"));
        Toast.makeText(this, "Khuếch đại cử chỉ x" + SCALE_FACTOR, Toast.LENGTH_SHORT).show();
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
            isTouching = true;
            vibrate(4);
            return;
        }

        if (action == MotionEvent.ACTION_MOVE && isTouching) {
            float dx = x - lastX;
            float dy = y - lastY;

            if (Math.abs(dx) < MIN_DELTA && Math.abs(dy) < MIN_DELTA) {
                return;
            }

            float boostedX = x + dx * SCALE_FACTOR;
            float boostedY = y + dy * SCALE_FACTOR;

            sendSwipe(lastX, lastY, boostedX, boostedY);

            lastX = x;
            lastY = y;
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            isTouching = false;
        }
    }

    private void sendSwipe(float x1, float y1, float x2, float y2) {
        Intent intent = new Intent("com.gesture.assist.SWIPE");
        intent.putExtra("x1", x1);
        intent.putExtra("y1", y1);
        intent.putExtra("x2", x2);
        intent.putExtra("y2", y2);
        intent.putExtra("duration", 1);
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
