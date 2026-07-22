package com.gesture.assist;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.os.RemoteException;
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
    private float screenWidth, screenHeight;
    private float lastX, lastY;
    private IInputInjector injectorBinder;
    private boolean serviceBound = false;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            injectorBinder = IInputInjector.Stub.asInterface(service);
            serviceBound = true;
            Toast.makeText(GestureAssistService.this, "Đã kết nối Injector", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            injectorBinder = null;
            serviceBound = false;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        // Bind tới ShizukuInjectorService
        Intent intent = new Intent(this, ShizukuInjectorService.class);
        bindService(intent, connection, Context.BIND_AUTO_CREATE);

        createOverlay();
        Toast.makeText(this, "Khuếch đại x" + SCALE_FACTOR, Toast.LENGTH_SHORT).show();
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
        if (!isActive || !serviceBound || injectorBinder == null) return;

        int action = rawEvent.getActionMasked();
        float rawX = rawEvent.getRawX();
        float rawY = rawEvent.getRawY();

        if (action == MotionEvent.ACTION_DOWN) {
            lastX = rawX;
            lastY = rawY;
            vibrate();
            try {
                injectorBinder.inject(rawX, rawY, MotionEvent.ACTION_DOWN);
            } catch (RemoteException e) { e.printStackTrace(); }
            return;
        }

        if (action == MotionEvent.ACTION_MOVE || action == MotionEvent.ACTION_UP) {
            float deltaX = (rawX - lastX) * SCALE_FACTOR;
            float deltaY = (rawY - lastY) * SCALE_FACTOR;
            float boostedX = Math.max(0, Math.min(screenWidth, rawX + deltaX));
            float boostedY = Math.max(0, Math.min(screenHeight, rawY + deltaY));

            try {
                injectorBinder.inject(boostedX, boostedY, MotionEvent.ACTION_MOVE);
                if (action == MotionEvent.ACTION_UP) {
                    injectorBinder.inject(boostedX, boostedY, MotionEvent.ACTION_UP);
                }
            } catch (RemoteException e) { e.printStackTrace(); }

            lastX = rawX;
            lastY = rawY;
        }
    }

    private void vibrate() {
        if (vibrator != null) {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(4, 20));
            } else {
                vibrator.vibrate(4);
            }
        }
    }

    @Override
    public void onDestroy() {
        isActive = false;
        if (overlay != null) {
            try { wm.removeView(overlay); } catch (Exception ignored) {}
            overlay = null;
        }
        if (serviceBound) {
            unbindService(connection);
            serviceBound = false;
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
