package com.gesture.assist;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.provider.Settings;
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

    private float sensitivity = 20000.0f;
    private int cursorSize = 40;

    @Override
    public void onCreate() {
        super.onCreate();
        Toast.makeText(this, "Service onCreate", Toast.LENGTH_SHORT).show();

        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        if (Settings.canDrawOverlays(this)) {
            createOverlay();
            createCursorView();
            Toast.makeText(this, "Overlay + Cursor created", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "⚠️ Cần quyền Overlay!", Toast.LENGTH_LONG).show();
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
        cursorView.setBackgroundColor(0xFFFF0000);
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
        if (!Settings.canDrawOverlays(this)) return;

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
            float dx = (x - lastX) * sensitivity;
            float dy = (y - lastY) * sensitivity;
            float newX = Math.max(0, Math.min(screenWidth - cursorSize, cursorX + dx));
            float newY = Math.max(0, Math.min(screenHeight - cursorSize, cursorY + dy));
            cursorX = newX;
            cursorY = newY;
            moveCursor();
            lastX = x;
            lastY = y;
            return;
        }

        if (action == MotionEvent.ACTION_UP && isTrackpadActive) {
            isTrackpadActive = false;
            hideCursor();
        }
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
        if (overlay != null) {
            try { wm.removeView(overlay); } catch (Exception ignored) {}
        }
        if (cursorView != null) {
            try { wm.removeView(cursorView); } catch (Exception ignored) {}
        }
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
