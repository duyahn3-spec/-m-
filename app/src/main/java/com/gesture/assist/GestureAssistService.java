package com.gesture.assist;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

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

    // HỆ SỐ NHẠY - TĂNG LÊN 10, 20 ĐỂ XOAY TÍT
    private static final float SENSITIVITY = 10.0f;

    @Override
    public void onCreate() {
        super.onCreate();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        createOverlay();
        createCursorView();

        Toast.makeText(this, "Địt con mẹ! rút cu ra! Sukak đi😵!", Toast.LENGTH_LONG).show();
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
        cursorView.setBackgroundColor(0xFFFF0000);
        cursorView.setVisibility(View.GONE);

        cursorParams = new WindowManager.LayoutParams(
                50, 50,
                Build.VERSION.SDK_INT >= 26 ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        cursorParams.gravity = Gravity.TOP | Gravity.START;
        wm.addView(cursorView, cursorParams);
    }

    private void processTouch(MotionEvent event) {
        int action = event.getActionMasked();
        float x = event.getRawX();
        float y = event.getRawY();

        boolean isInTrackpad = y > screenHeight * 0.35;

        if (action == MotionEvent.ACTION_DOWN) {
            if (isInTrackpad) {
                isTrackpadActive = true;
                lastX = x;
                lastY = y;
                cursorX = x;
                cursorY = Math.max(0, y - screenHeight * 0.3f);
                showCursor(cursorX, cursorY);
                vibrate(8);
            }
            return;
        }

        if (action == MotionEvent.ACTION_MOVE && isTrackpadActive) {
            float dx = (x - lastX) * SENSITIVITY;
            float dy = (y - lastY) * SENSITIVITY;
            cursorX = Math.max(0, Math.min(screenWidth, cursorX + dx));
            cursorY = Math.max(0, Math.min(screenHeight, cursorY + dy));
            moveCursor(cursorX, cursorY);
            lastX = x;
            lastY = y;
            return;
        }

        if (action == MotionEvent.ACTION_UP && isTrackpadActive) {
            isTrackpadActive = false;
            clickAt(cursorX, cursorY);
            vibrate(12);
            hideCursor();
        }
    }

    private void showCursor(float x, float y) {
        isCursorVisible = true;
        cursorView.setVisibility(View.VISIBLE);
        cursorParams.x = (int) x - 25;
        cursorParams.y = (int) y - 25;
        wm.updateViewLayout(cursorView, cursorParams);
    }

    private void moveCursor(float x, float y) {
        if (!isCursorVisible) return;
        cursorParams.x = (int) x - 25;
        cursorParams.y = (int) y - 25;
        wm.updateViewLayout(cursorView, cursorParams);
    }

    private void hideCursor() {
        isCursorVisible = false;
        cursorView.setVisibility(View.GONE);
    }

    private void clickAt(float x, float y) {
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
