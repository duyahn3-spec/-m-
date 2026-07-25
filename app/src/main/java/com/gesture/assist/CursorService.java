package com.cuto.shizuku.full;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.Context;
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

public class CursorService extends AccessibilityService {
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

    // SENSITIVITY = 10000 - SIÊU NHẠY NHƯ CHUỘT
    private float sensitivity = 10000.0f;
    private int cursorSize = 40;

    private Handler handler = new Handler();

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

        Toast.makeText(this, "🔥 Cuto Khủng Bố (siêu nhạy) đã sẵn sàng!", Toast.LENGTH_LONG).show();
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
            cursorX = Math.max(0, Math.min(screenWidth - cursorSize, cursorX + dx));
            cursorY = Math.max(0, Math.min(screenHeight - cursorSize, cursorY + dy));
            moveCursor();
            lastX = x;
            lastY = y;
            return;
        }

        if (action == MotionEvent.ACTION_UP && isTrackpadActive) {
            isTrackpadActive = false;
            // CLICK TẠI VỊ TRÍ CON TRỎ - KHÔNG BỊ GAME CHẶN
            clickAt(cursorX + cursorSize/2, cursorY + cursorSize/2);
            vibrate(10);
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

    // DÙNG dispatchGesture ĐỂ CLICK - HỢP LỆ, KHÔNG BỊ CHẶN
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
                return true; // Nuốt sự kiện, không truyền xuống game
            }
            return false;
        }

        interface TouchInterceptor {
            void onTouch(MotionEvent event);
        }
    }
}
