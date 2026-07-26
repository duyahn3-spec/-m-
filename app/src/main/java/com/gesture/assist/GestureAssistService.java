package com.gesture.assist;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

public class GestureAssistService extends AccessibilityService {
    private WindowManager wm;
    private OverlayView overlay;
    private ImageView cursorView;
    private WindowManager.LayoutParams cursorParams;

    private float lastX, lastY;
    private float cursorX, cursorY;
    private boolean isCursorVisible = false;
    private boolean isTrackpadActive = false;
    private int screenWidth, screenHeight;

    // ===== BUFF TỐI ĐA TOÀN BỘ =====
    private float sensitivity = 100000000000.0f;   // 100 tỷ
    private float deadZone = 0.0f;
    private float acceleration = Float.MAX_VALUE;
    private int cursorSize = 1;

    private Handler handler = new Handler();
    private SharedPreferences prefs;
    private boolean isSuperTouchOn = true;
    private boolean isPointerSpeedOn = true;
    private boolean isDispatchOn = false;
    private int currentDensity = 100;

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        prefs = getSharedPreferences("gamepad_settings", MODE_PRIVATE);
        loadSettings();

        createOverlay();
        createCursorView();
        applyOptimizations();

        Toast.makeText(this, "🔥 Suk Toạc BQĐ!", Toast.LENGTH_LONG).show();
    }

    private void loadSettings() {
        sensitivity = prefs.getInt("sensitivity", 20000);
        currentDensity = prefs.getInt("density", 100);
        isSuperTouchOn = prefs.getBoolean("super_touch", true);
        isPointerSpeedOn = prefs.getBoolean("pointer_speed", true);
        isDispatchOn = prefs.getBoolean("dispatch", false);
    }

    private void applyOptimizations() {
        new Thread(() -> {
            try {
                if (isSuperTouchOn) {
                    runCommand("setprop ro.min_pointer_dur 0");
                    runCommand("setprop debug.input.smoothing 0");
                    runCommand("setprop windowsmgr.max_events_per_sec 9999");
                    runCommand("setprop touch.pressure.scale 0.0");
                    runCommand("setprop touch.size.scale 0.0");
                    runCommand("setprop touch.distance.scale 0.0");
                    runCommand("setprop touch.size.bias 0.0");
                    runCommand("setprop persist.sys.touch.sampling.rate 10000");
                }
                if (isPointerSpeedOn) {
                    runCommand("settings put system pointer_speed 7");
                }
                runCommand("wm density " + currentDensity);
                runCommand("wm scaling off");
                runCommand("settings put global force_gpu_rendering 1");
                runCommand("settings put system window_animation_scale 0.0");
                runCommand("settings put system transition_animation_scale 0.0");
                runCommand("settings put system animator_duration_scale 0.0");
                runCommand("settings put system long_press_timeout 50");
                runCommand("settings put system scroll_friction 0.0");
                runCommand("cmd activity kill-all");
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void runCommand(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                        WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP;
        wm.addView(overlay, params);
    }

    private void createCursorView() {
        cursorView = new ImageView(this);
        cursorView.setBackgroundColor(0xFF00FF00);
        cursorView.setVisibility(View.GONE);
        cursorParams = new WindowManager.LayoutParams(
                cursorSize, cursorSize,
                Build.VERSION.SDK_INT >= 26 ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        cursorParams.gravity = Gravity.TOP | Gravity.START;
        cursorParams.x = 0;
        cursorParams.y = 0;
        wm.addView(cursorView, cursorParams);
    }

    private void processTouch(MotionEvent event) {
        int action = event.getActionMasked();
        float x = event.getRawX();
        float y = event.getRawY();

        if (action == MotionEvent.ACTION_DOWN) {
            isTrackpadActive = true;
            lastX = x;
            lastY = y;
            cursorX = Math.min(x, screenWidth - cursorSize);
            cursorY = Math.min(y, screenHeight - cursorSize);
            showCursor();
            return;
        }

        if (action == MotionEvent.ACTION_MOVE && isTrackpadActive) {
            float dx = (x - lastX) * sensitivity * acceleration;
            float dy = (y - lastY) * sensitivity * acceleration;

            float newX = Math.max(0, Math.min(screenWidth - cursorSize, cursorX + dx));
            float newY = Math.max(0, Math.min(screenHeight - cursorSize, cursorY + dy));

            if (isDispatchOn) {
                sendGamepadMove(cursorX + cursorSize/2, cursorY + cursorSize/2,
                                newX + cursorSize/2, newY + cursorSize/2);
            }

            cursorX = newX;
            cursorY = newY;
            moveCursor();
            lastX = x;
            lastY = y;

            try {
                Thread.sleep(0);
            } catch (InterruptedException ignored) {}
            return;
        }

        if (action == MotionEvent.ACTION_UP && isTrackpadActive) {
            isTrackpadActive = false;
            if (isDispatchOn) {
                clickGamepad(cursorX + cursorSize/2, cursorY + cursorSize/2);
            }
            hideCursor();
        }
    }

    private void sendGamepadMove(float x1, float y1, float x2, float y2) {
        android.accessibilityservice.GestureDescription.Builder builder =
            new android.accessibilityservice.GestureDescription.Builder();
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        builder.addStroke(new android.accessibilityservice.GestureDescription
            .StrokeDescription(path, 0, 0));
        dispatchGesture(builder.build(), null, null);
    }

    private void clickGamepad(float x, float y) {
        android.accessibilityservice.GestureDescription.Builder builder =
            new android.accessibilityservice.GestureDescription.Builder();
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(x, y);
        path.lineTo(x + 1, y + 1);
        builder.addStroke(new android.accessibilityservice.GestureDescription
            .StrokeDescription(path, 0, 0));
        dispatchGesture(builder.build(), null, null);
    }

    private void showCursor() {
        isCursorVisible = true;
        cursorView.setVisibility(View.VISIBLE);
        cursorParams.x = (int) cursorX;
        cursorParams.y = (int) cursorY;
        wm.updateViewLayout(cursorView, cursorParams);
    }

    private void moveCursor() {
        if (!isCursorVisible) return;
        cursorParams.x = (int) cursorX;
        cursorParams.y = (int) cursorY;
        wm.updateViewLayout(cursorView, cursorParams);
    }

    private void hideCursor() {
        isCursorVisible = false;
        cursorView.setVisibility(View.GONE);
    }

    @Override
    public void onDestroy() {
        if (overlay != null) wm.removeView(overlay);
        if (cursorView != null) wm.removeView(cursorView);
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    // ===== NESTED CLASS OverlayView =====
    private static class OverlayView extends View {
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
            if (interceptor != null) {
                interceptor.onTouch(event);
                return true;
            }
            return false;
        }

        interface TouchInterceptor {
            void onTouch(MotionEvent event);
        }
    }
}= null) wm.removeView(cursorView);
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

    private static class OverlayView extends View {
        private TouchInterceptor interceptor;
        public OverlayView(Context context) { super(context); setFocusable(false); }
        public void setTouchInterceptor(TouchInterceptor interceptor) { this.interceptor = interceptor; }
        @Override
        public boolean onTouchEvent(MotionEvent event) {
            if (interceptor != null) {
                interceptor.onTouch(event);
                return true;
            }
            return false;
        }
        interface TouchInterceptor { void onTouch(MotionEvent event); }
    }
}
