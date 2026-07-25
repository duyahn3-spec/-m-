package com.gesture.assist;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;
import java.util.Random;

public class GestureAssistService extends AccessibilityService {
    private WindowManager wm;
    private OverlayView overlay;
    private Vibrator vibrator;
    private ImageView cursorView;
    private WindowManager.LayoutParams cursorParams;

    private float lastX, lastY;
    private float cursorX, cursorY;
    private boolean isCursorVisible = false;
    private boolean isTrackpadActive = false;
    private int screenWidth, screenHeight;

    private float sensitivity = 20000.0f;
    private int cursorSize = 40;
    private Handler handler = new Handler();
    private Random random = new Random();
    private float deadZone = 0.05f;
    private float acceleration = 1.5f;

    @Override
    public void onCreate() {
        super.onCreate();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        // Đọc độ nhạy đã lưu
        SharedPreferences prefs = getSharedPreferences("gamepad_settings", MODE_PRIVATE);
        sensitivity = prefs.getInt("sensitivity", 20000);

        createOverlay();
        createCursorView();

        Toast.makeText(this, "🎮 Cuto Gamepad (Sensitivity: " + (int)sensitivity + ")", Toast.LENGTH_LONG).show();
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

            float magnitude = (float) Math.sqrt(dx*dx + dy*dy);
            if (magnitude < deadZone * 100) {
                dx = 0;
                dy = 0;
            }

            float noiseX = (random.nextFloat() - 0.5f) * 0.3f;
            float noiseY = (random.nextFloat() - 0.5f) * 0.3f;
            dx += noiseX;
            dy += noiseY;

            float newX = Math.max(0, Math.min(screenWidth - cursorSize, cursorX + dx));
            float newY = Math.max(0, Math.min(screenHeight - cursorSize, cursorY + dy));

            sendGamepadMove(cursorX + cursorSize/2, cursorY + cursorSize/2, newX + cursorSize/2, newY + cursorSize/2);

            cursorX = newX;
            cursorY = newY;
            moveCursor();
            lastX = x;
            lastY = y;

            try {
                Thread.sleep(5 + random.nextInt(5));
            } catch (InterruptedException ignored) {}
            return;
        }

        if (action == MotionEvent.ACTION_UP && isTrackpadActive) {
            isTrackpadActive = false;
            clickGamepad(cursorX + cursorSize/2, cursorY + cursorSize/2);
            vibrate(8);
            hideCursor();
        }
    }

    private void sendGamepadMove(float x1, float y1, float x2, float y2) {
        Path path = new Path();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 1));
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

    private void clickGamepad(float x, float y) {
        Path path = new Path();
        path.moveTo(x, y);
        path.lineTo(x + 1, y + 1);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 1));
        dispatchGesture(builder.build(), null, null);
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
        if (overlay != null) wm.removeView(overlay);
        if (cursorView != null) wm.removeView(cursorView);
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) {}

    @Override
    public void onInterrupt() {}

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
                            }
