package com.gesture.assist;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import rikka.shizuku.Shizuku;

public class GestureAssistService extends AccessibilityService {

    private static final String TAG = "GestureService";
    private static final float SCALE_FACTOR = 1440.0f;

    private WindowManager wm;
    private OverlayView overlay;
    private ScheduledExecutorService scheduler;
    private AtomicBoolean isActive = new AtomicBoolean(false);
    private Vibrator vibrator;
    private float screenWidth, screenHeight;
    private float lastX, lastY;
    private SecureRandom random;

    private IBinder inputManagerBinder;
    private Method injectMethod;

    @Override
    public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) {}

    @Override
    public void onInterrupt() { deactivate(); }

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

        if (Shizuku.pingBinder()) {
            try {
                inputManagerBinder = Shizuku.getSystemService("input");
                if (inputManagerBinder != null) {
                    injectMethod = inputManagerBinder.getClass().getMethod(
                        "injectInputEvent", MotionEvent.class, int.class
                    );
                    Toast.makeText(this, "✅ Shizuku + InputManager OK", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Shizuku error", e);
                Toast.makeText(this, "❌ Lỗi Shizuku: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "❌ Shizuku chưa chạy!", Toast.LENGTH_LONG).show();
        }

        createOverlay();
        Toast.makeText(this, "🔥 1440x + Shizuku", Toast.LENGTH_SHORT).show();
        activate();
    }

    private void createOverlay() {
        if (overlay != null) {
            try { wm.removeView(overlay); } catch (Exception ignored) {}
        }
        overlay = new OverlayView(this);
        overlay.setTouchInterceptor(new TouchInterceptor());

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP;
        wm.addView(overlay, params);
    }

    private class TouchInterceptor implements OverlayView.TouchInterceptor {
        @Override
        public void onTouch(MotionEvent event) {
            processTouch(event);
        }
    }

    private void processTouch(MotionEvent rawEvent) {
        if (!isActive.get() || injectMethod == null) return;

        int action = rawEvent.getActionMasked();
        float rawX = rawEvent.getRawX();
        float rawY = rawEvent.getRawY();

        float jitterX = (random.nextFloat() - 0.5f) * 0.001f * 2;
        float jitterY = (random.nextFloat() - 0.5f) * 0.001f * 2;
        float noisyX = rawX + jitterX;
        float noisyY = rawY + jitterY;

        float deltaX = (noisyX - lastX) * SCALE_FACTOR;
        float deltaY = (noisyY - lastY) * SCALE_FACTOR;

        float boostedX = lastX + deltaX;
        float boostedY = lastY + deltaY;

        boostedX = Math.max(0, Math.min(screenWidth, boostedX));
        boostedY = Math.max(0, Math.min(screenHeight, boostedY));

        if (action == MotionEvent.ACTION_DOWN && vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(4, 20));
            } else {
                vibrator.vibrate(4);
            }
        }

        injectTouch(noisyX, noisyY, MotionEvent.ACTION_DOWN);
        injectTouch(boostedX, boostedY, MotionEvent.ACTION_MOVE);
        injectTouch(boostedX, boostedY, MotionEvent.ACTION_UP);

        lastX = noisyX;
        lastY = noisyY;
    }

    private void injectTouch(float x, float y, int action) {
        if (injectMethod == null) return;
        long now = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(
            now, now, action,
            x, y, 1.0f, 1.0f, 0, 0,
            InputDevice.SOURCE_TOUCHSCREEN, 0
        );
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);

        try {
            injectMethod.invoke(inputManagerBinder, event, 0);
        } catch (Exception e) {
            Log.e(TAG, "Inject failed", e);
        } finally {
            event.recycle();
        }
    }

    private void activate() { isActive.set(true); }

    private void deactivate() {
        isActive.set(false);
        if (overlay != null) {
            try { wm.removeView(overlay); } catch (Exception ignored) {}
            overlay = null;
        }
    }

    @Override
    public void onDestroy() {
        deactivate();
        scheduler.shutdown();
        super.onDestroy();
    }

    private static class OverlayView extends android.view.View {
        private TouchInterceptor interceptor;

        public OverlayView(Context context) {
            super(context);
            setFocusable(false);
            setKeepScreenOn(true);
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

        public interface TouchInterceptor {
            void onTouch(MotionEvent event);
        }
    }
}        createOverlay();
        Toast.makeText(this, "🔥 1440x + Shizuku", Toast.LENGTH_SHORT).show();
        activate();
    }

    private void createOverlay() {
        if (overlay != null) {
            try { wm.removeView(overlay); } catch (Exception ignored) {}
        }
        overlay = new OverlayView(this);
        overlay.setTouchInterceptor(new TouchInterceptor());

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
            WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH |
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP;
        wm.addView(overlay, params);
    }

    private class TouchInterceptor implements OverlayView.TouchInterceptor {
        @Override
        public void onTouch(MotionEvent event) {
            processTouch(event);
        }
    }

    private void processTouch(MotionEvent rawEvent) {
        if (!isActive.get() || injectMethod == null) return;

        int action = rawEvent.getActionMasked();
        float rawX = rawEvent.getRawX();
        float rawY = rawEvent.getRawY();

        float jitterX = (random.nextFloat() - 0.5f) * 0.001f * 2;
        float jitterY = (random.nextFloat() - 0.5f) * 0.001f * 2;
        float noisyX = rawX + jitterX;
        float noisyY = rawY + jitterY;

        float deltaX = (noisyX - lastX) * SCALE_FACTOR;
        float deltaY = (noisyY - lastY) * SCALE_FACTOR;

        float boostedX = lastX + deltaX;
        float boostedY = lastY + deltaY;

        boostedX = Math.max(0, Math.min(screenWidth, boostedX));
        boostedY = Math.max(0, Math.min(screenHeight, boostedY));

        if (action == MotionEvent.ACTION_DOWN && vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(4, 20));
            } else {
                vibrator.vibrate(4);
            }
        }

        injectTouch(noisyX, noisyY, MotionEvent.ACTION_DOWN);
        injectTouch(boostedX, boostedY, MotionEvent.ACTION_MOVE);
        injectTouch(boostedX, boostedY, MotionEvent.ACTION_UP);

        lastX = noisyX;
        lastY = noisyY;
    }

    private void injectTouch(float x, float y, int action) {
        if (injectMethod == null) return;
        long now = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(
            now, now, action,
            x, y, 1.0f, 1.0f, 0, 0,
            InputDevice.SOURCE_TOUCHSCREEN, 0
        );
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);

        try {
            injectMethod.invoke(inputManagerBinder, event, 0);
        } catch (Exception e) {
            Log.e(TAG, "Inject failed", e);
        } finally {
            event.recycle();
        }
    }

    private void activate() { isActive.set(true); }

    private void deactivate() {
        isActive.set(false);
        if (overlay != null) {
            try { wm.removeView(overlay); } catch (Exception ignored) {}
            overlay = null;
        }
    }

    @Override
    public void onDestroy() {
        deactivate();
        scheduler.shutdown();
        super.onDestroy();
    }

    private static class OverlayView extends android.view.View {
        private TouchInterceptor interceptor;

        public OverlayView(Context context) {
            super(context);
            setFocusable(false);
            setKeepScreenOn(true);
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

        public interface TouchInterceptor {
            void onTouch(MotionEvent event);
        }
    }
}        @Override
        public void onTouch(MotionEvent event) {
            processTouch(event);
        }
    }

    private void processTouch(MotionEvent rawEvent) {
        if (!isActive.get() || injectMethod == null) return;

        int action = rawEvent.getActionMasked();
        float rawX = rawEvent.getRawX();
        float rawY = rawEvent.getRawY();

        float jitterX = (random.nextFloat() - 0.5f) * 0.001f * 2;
        float jitterY = (random.nextFloat() - 0.5f) * 0.001f * 2;
        float noisyX = rawX + jitterX;
        float noisyY = rawY + jitterY;

        float deltaX = (noisyX - lastX) * SCALE_FACTOR;
        float deltaY = (noisyY - lastY) * SCALE_FACTOR;

        float boostedX = lastX + deltaX;
        float boostedY = lastY + deltaY;

        boostedX = Math.max(0, Math.min(screenWidth, boostedX));
        boostedY = Math.max(0, Math.min(screenHeight, boostedY));

        if (action == MotionEvent.ACTION_DOWN && vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(4, 20));
            } else {
                vibrator.vibrate(4);
            }
        }

        injectTouch(noisyX, noisyY, MotionEvent.ACTION_DOWN);
        injectTouch(boostedX, boostedY, MotionEvent.ACTION_MOVE);
        injectTouch(boostedX, boostedY, MotionEvent.ACTION_UP);

        lastX = noisyX;
        lastY = noisyY;
    }

    private void injectTouch(float x, float y, int action) {
        if (injectMethod == null) return;
        long now = SystemClock.uptimeMillis();
        MotionEvent event = MotionEvent.obtain(
            now, now, action,
            x, y, 1.0f, 1.0f, 0, 0,
            InputDevice.SOURCE_TOUCHSCREEN, 0
        );
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);

        try {
            injectMethod.invoke(inputManagerBinder, event, 0);
        } catch (Exception e) {
            Log.e(TAG, "Inject failed", e);
        } finally {
            event.recycle();
        }
    }

    private void activate() { isActive.set(true); }

    private void deactivate() {
        isActive.set(false);
        if (overlay != null) {
            try { wm.removeView(overlay); } catch (Exception ignored) {}
            overlay = null;
        }
    }

    @Override
    public void onDestroy() {
        deactivate();
        scheduler.shutdown();
        super.onDestroy();
    }

    private static class OverlayView extends android.view.View {
        private TouchInterceptor interceptor;

        public OverlayView(Context context) {
            super(context);
            setFocusable(false);
            setKeepScreenOn(true);
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

        public interface TouchInterceptor {
            void onTouch(MotionEvent event);
        }
    }
}
