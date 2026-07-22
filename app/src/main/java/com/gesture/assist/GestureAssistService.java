package com.gesture.assist;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.os.RemoteException;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Toast;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.os.Build;

import rikka.shizuku.Shizuku;

public class GestureAssistService extends AccessibilityService {
    private static final String TAG = "GestureAssistService";
    private static final float SCALE = 20f; // Mày tăng lên nếu muốn

    private WindowManager wm;
    private OverlayView overlay;
    private boolean isActive = false;
    private Vibrator vibrator;
    private float lastX, lastY;
    private IInputInjector injector;
    private boolean bound = false;

    private final Shizuku.UserServiceConnection connection = new Shizuku.UserServiceConnection() {
        @Override
        public void onUserServiceConnected(IBinder binder) {
            injector = IInputInjector.Stub.asInterface(binder);
            bound = true;
            Toast.makeText(GestureAssistService.this, "Đã kết nối UserService", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onUserServiceDisconnected() {
            injector = null;
            bound = false;
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        // Bind UserService
        Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(
                new ComponentName(this, MyUserService.class))
                .daemon(false)
                .process(":my_user_service")
                .version(1);
        Shizuku.bindUserService(args, connection);

        createOverlay();
        isActive = true;
    }

    private void createOverlay() {
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
        wm.addView(overlay, params);
    }

    private void processTouch(MotionEvent event) {
        if (!isActive || !bound || injector == null) return;

        float x = event.getRawX();
        float y = event.getRawY();

        if (event.getActionMasked() == MotionEvent.ACTION_DOWN) {
            lastX = x;
            lastY = y;
            vibrate();
            try {
                injector.injectEvent(x, y, MotionEvent.ACTION_DOWN, InputDevice.SOURCE_TOUCHSCREEN);
            } catch (RemoteException ignored) {}
            return;
        }

        if (event.getActionMasked() == MotionEvent.ACTION_MOVE ||
            event.getActionMasked() == MotionEvent.ACTION_UP) {

            float dx = (x - lastX) * SCALE;
            float dy = (y - lastY) * SCALE;
            float boostedX = Math.max(0, Math.min(getResources().getDisplayMetrics().widthPixels, x + dx));
            float boostedY = Math.max(0, Math.min(getResources().getDisplayMetrics().heightPixels, y + dy));

            try {
                injector.injectEvent(boostedX, boostedY, MotionEvent.ACTION_MOVE, InputDevice.SOURCE_TOUCHSCREEN);
                if (event.getActionMasked() == MotionEvent.ACTION_UP) {
                    injector.injectEvent(boostedX, boostedY, MotionEvent.ACTION_UP, InputDevice.SOURCE_TOUCHSCREEN);
                }
            } catch (RemoteException ignored) {}

            lastX = x;
            lastY = y;
        }
    }

    private void vibrate() {
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(4, 20));
            } else {
                vibrator.vibrate(4);
            }
        }
    }

    @Override
    public void onDestroy() {
        isActive = false;
        if (overlay != null) wm.removeView(overlay);
        if (bound) {
            Shizuku.unbindUserService(connection, true);
            bound = false;
        }
        super.onDestroy();
    }

    // OverlayView inner class (giữ nguyên)
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

    @Override
    public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}
}
