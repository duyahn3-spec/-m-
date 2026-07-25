package com.cuto.shizuku.full;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

public class CursorService extends Service {
    private WindowManager wm;
    private ImageView cursorView;
    private WindowManager.LayoutParams cursorParams;
    private View overlayView;
    private WindowManager.LayoutParams overlayParams;

    private float cursorX = 500, cursorY = 500;
    private float lastX, lastY;
    private boolean isTrackpadActive = false;
    private boolean isServiceActive = false;
    private float sensitivity = 1000.0f;
    private int cursorSize = 40;
    private String triggerEdge = "Phải";

    private Handler handler = new Handler();
    private BroadcastReceiver settingsReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        createCursor();
        createOverlay();
        isServiceActive = true;
        Toast.makeText(this, "🔥 Cuto Khủng Bố đã bật! (Sensitivity=1000)", Toast.LENGTH_SHORT).show();

        settingsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.cuto.shizuku.full.UPDATE_SETTINGS".equals(intent.getAction())) {
                    float sensitivity = intent.getFloatExtra("sensitivity", -1);
                    int cursorSize = intent.getIntExtra("cursorSize", -1);
                    String triggerEdge = intent.getStringExtra("triggerEdge");
                    if (sensitivity > 0) {
                        CursorService.this.sensitivity = sensitivity;
                    }
                    if (cursorSize > 0) {
                        CursorService.this.cursorSize = cursorSize;
                        cursorParams.width = cursorSize;
                        cursorParams.height = cursorSize;
                        wm.updateViewLayout(cursorView, cursorParams);
                    }
                    if (triggerEdge != null) {
                        CursorService.this.triggerEdge = triggerEdge;
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter("com.cuto.shizuku.full.UPDATE_SETTINGS");
        registerReceiver(settingsReceiver, filter);
    }

    private void createCursor() {
        cursorView = new ImageView(this);
        cursorView.setBackgroundColor(0xFFFF0000);
        cursorView.setVisibility(View.INVISIBLE);

        cursorParams = new WindowManager.LayoutParams(
                cursorSize, cursorSize,
                Build.VERSION.SDK_INT >= 26 ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        cursorParams.gravity = Gravity.TOP | Gravity.START;
        cursorParams.x = (int) cursorX - cursorSize/2;
        cursorParams.y = (int) cursorY - cursorSize/2;
        wm.addView(cursorView, cursorParams);
    }

    private void createOverlay() {
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_trackpad, null);
        overlayView.setOnTouchListener((v, event) -> {
            if (!isServiceActive) return false;
            handleTouch(event);
            return true;
        });

        overlayParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= 26 ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT
        );
        overlayParams.gravity = Gravity.TOP;
        overlayView.setVisibility(View.INVISIBLE);
        wm.addView(overlayView, overlayParams);
    }

    private void handleTouch(MotionEvent event) {
        int action = event.getActionMasked();
        float x = event.getRawX();
        float y = event.getRawY();

        boolean isInTrigger = checkTriggerArea(x, y);

        if (action == MotionEvent.ACTION_DOWN) {
            if (isInTrigger) {
                isTrackpadActive = true;
                lastX = x;
                lastY = y;
                cursorX = Math.min(x, 1080 - cursorSize);
                cursorY = Math.min(y, 1920 - cursorSize);
                showCursor();
            }
            return;
        }

        if (action == MotionEvent.ACTION_MOVE && isTrackpadActive) {
            float dx = (x - lastX) * sensitivity;
            float dy = (y - lastY) * sensitivity;
            cursorX = Math.max(0, Math.min(1080 - cursorSize, cursorX + dx));
            cursorY = Math.max(0, Math.min(1920 - cursorSize, cursorY + dy));
            updateCursorPosition();
            lastX = x;
            lastY = y;
            return;
        }

        if (action == MotionEvent.ACTION_UP && isTrackpadActive) {
            isTrackpadActive = false;
            clickAt(cursorX + cursorSize/2, cursorY + cursorSize/2);
            hideCursor();
        }
    }

    private boolean checkTriggerArea(float x, float y) {
        int triggerWidth = 80;
        if ("Trái".equals(triggerEdge)) {
            return x < triggerWidth;
        } else if ("Phải".equals(triggerEdge)) {
            return x > 1080 - triggerWidth;
        } else if ("Dưới".equals(triggerEdge)) {
            return y > 1920 - 120;
        } else {
            return x < triggerWidth || x > 1080 - triggerWidth;
        }
    }

    private void showCursor() {
        cursorView.setVisibility(View.VISIBLE);
    }

    private void hideCursor() {
        cursorView.setVisibility(View.INVISIBLE);
    }

    private void updateCursorPosition() {
        cursorParams.x = (int) cursorX;
        cursorParams.y = (int) cursorY;
        wm.updateViewLayout(cursorView, cursorParams);
    }

    private void clickAt(float x, float y) {
        if (!Shizuku.pingBinder() || Shizuku.checkSelfPermission() != 0) {
            Toast.makeText(this, "Shizuku chưa sẵn sàng!", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                Object inputManager = Class.forName("android.hardware.input.InputManager")
                        .getMethod("getInstance").invoke(null);
                long downTime = SystemClock.uptimeMillis();

                MotionEvent downEvent = MotionEvent.obtain(downTime, downTime + 50,
                        MotionEvent.ACTION_DOWN, x, y, 0);
                downEvent.setSource(InputDevice.SOURCE_TOUCHSCREEN);
                inputManager.getClass()
                        .getMethod("injectInputEvent", MotionEvent.class, int.class)
                        .invoke(inputManager, downEvent, 0);
                downEvent.recycle();

                MotionEvent upEvent = MotionEvent.obtain(downTime, downTime + 100,
                        MotionEvent.ACTION_UP, x, y, 0);
                upEvent.setSource(InputDevice.SOURCE_TOUCHSCREEN);
                inputManager.getClass()
                        .getMethod("injectInputEvent", MotionEvent.class, int.class)
                        .invoke(inputManager, upEvent, 0);
                upEvent.recycle();

            } catch (Exception e) {
                e.printStackTrace();
                handler.post(() -> Toast.makeText(CursorService.this, "Lỗi click: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    @Override
    public void onDestroy() {
        isServiceActive = false;
        if (cursorView != null) wm.removeView(cursorView);
        if (overlayView != null) wm.removeView(overlayView);
        if (settingsReceiver != null) unregisterReceiver(settingsReceiver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
