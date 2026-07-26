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
import java.io.BufferedReader;
import java.io.InputStreamReader;

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
    private Handler handler = new Handler();
    private SharedPreferences prefs;

    // ===== CÁC THAM SỐ TỪ APP TÀU =====
    private boolean isSuperTouchOn = false;
    private boolean isPointerSpeedOn = true;
    private boolean isDispatchOn = false;
    private boolean isDensityOn = true;
    private boolean isSmoothingOff = true;
    private boolean isGPUForceOn = true;
    private boolean isGameModeOn = false;
    private int currentDensity = 240;
    private int touchLevel = 5; // 1-5: càng cao càng nhạy

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

        // ===== CHẠY TẤT CẢ TỐI ƯU CỦA BỌN TÀU =====
        applyAllOptimizations();

        Toast.makeText(this, "🔥 Cu to khủng bố - Full Tàu đã sẵn sàng!", Toast.LENGTH_LONG).show();
    }

    private void loadSettings() {
        sensitivity = prefs.getInt("sensitivity", 20000);
        currentDensity = prefs.getInt("density", 240);
        touchLevel = prefs.getInt("touch_level", 5);
        isSuperTouchOn = prefs.getBoolean("super_touch", true);
        isPointerSpeedOn = prefs.getBoolean("pointer_speed", true);
        isDispatchOn = prefs.getBoolean("dispatch", false);
        isDensityOn = prefs.getBoolean("density_on", true);
        isSmoothingOff = prefs.getBoolean("smoothing_off", true);
        isGPUForceOn = prefs.getBoolean("gpu_force", true);
        isGameModeOn = prefs.getBoolean("game_mode", false);
    }

    // ===== ÁP DỤNG TẤT CẢ KỸ THUẬT CỦA BỌN TÀU =====
    private void applyAllOptimizations() {
        new Thread(() -> {
            try {
                // === 1. SUPER TOUCH CORE ===
                if (isSuperTouchOn) {
                    // Lệnh cốt lõi: giảm delay giữa các lần cảm ứng
                    runCommand("setprop ro.min_pointer_dur 1");
                    runCommand("setprop debug.input.smoothing 0");
                    
                    // Tăng tốc xử lý sự kiện cảm ứng
                    runCommand("setprop windowsmgr.max_events_per_sec 300");
                    
                    // Giảm độ trễ touch (theo cơ chế của Super Touch)
                    runCommand("setprop touch.pressure.scale 0.001");
                    runCommand("setprop touch.size.scale 0.001");
                    
                    // Một số tham số nâng cao từ bọn Tàu
                    runCommand("setprop persist.sys.touch.pressure.scale 0.001");
                    runCommand("setprop persist.sys.touch.size.scale 0.001");
                    
                    // Tăng tần số lấy mẫu touch (nếu kernel hỗ trợ)
                    runCommand("setprop persist.sys.touch.sampling.rate 240");
                    
                    // Các tham số từ Super Touch v10.02: có thể set độ nhạy lên 100000
                    // Nhưng thực tế chỉ có tác dụng trên máy hỗ trợ
                    runCommand("setprop debug.touch.sensitivity 100000");
                }

                // === 2. POINTER SPEED (MAX) ===
                if (isPointerSpeedOn) {
                    runCommand("settings put system pointer_speed 7");
                }

                // === 3. DENSITY + SCALING (giảm DPI để tạo cảm giác vuốt xa) ===
                if (isDensityOn) {
                    runCommand("wm density " + currentDensity);
                    runCommand("wm scaling off");
                }

                // === 4. TẮT LÀM MỊN CẢM ỨNG ===
                if (isSmoothingOff) {
                    runCommand("setprop debug.input.smoothing 0");
                }

                // === 5. FORCE GPU RENDER ===
                if (isGPUForceOn) {
                    runCommand("settings put global force_gpu_rendering 1");
                }

                // === 6. GAME MODE (nếu có) ===
                if (isGameModeOn) {
                    // Ép game vào chế độ hiệu năng cao (nếu hệ thống hỗ trợ)
                    runCommand("cmd game mode performance com.dts.freefiremax");
                    runCommand("cmd game mode performance com.dts.freefire");
                }

                // === 7. TẮT ANIMATION (giảm trễ) ===
                runCommand("settings put system window_animation_scale 0.0");
                runCommand("settings put system transition_animation_scale 0.0");
                runCommand("settings put system animator_duration_scale 0.0");

                // === 8. GIẢM THỜI GIAN NHẤN GIỮ (theo ColorOS Game Assistant) ===
                runCommand("settings put system long_press_timeout 100");

                // === 9. TĂNG TỐC ĐỘ CUỘN ===
                runCommand("settings put system scroll_friction 0.001");

                // === 10. GIẾT APP RÁC ===
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

    // ===== OVERLAY + CURSOR (giữ nguyên) =====
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

    // ===== XỬ LÝ TOUCH (có dispatchGesture nếu bật) =====
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
            // Nhân sensitivity lên dựa trên touchLevel từ app Tàu
            float factor = 1.0f + (touchLevel * 0.5f); // Level 5 => factor 3.5
            float dx = (x - lastX) * sensitivity * factor;
            float dy = (y - lastY) * sensitivity * factor;

            float newX = Math.max(0, Math.min(screenWidth - cursorSize, cursorX + dx));
            float newY = Math.max(0, Math.min(screenHeight - cursorSize, cursorY + dy));

            // Nếu bật dispatchGesture, gửi cử chỉ ảo vào game
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
                Thread.sleep(5 + (int)(Math.random() * 5));
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

    // ===== dispatchGesture (nếu bật) =====
    private void sendGamepadMove(float x1, float y1, float x2, float y2) {
        android.accessibilityservice.GestureDescription.Builder builder =
            new android.accessibilityservice.GestureDescription.Builder();
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        builder.addStroke(new android.accessibilityservice.GestureDescription
            .StrokeDescription(path, 0, 1));
        dispatchGesture(builder.build(), null, null);
    }

    private void clickGamepad(float x, float y) {
        android.accessibilityservice.GestureDescription.Builder builder =
            new android.accessibilityservice.GestureDescription.Builder();
        android.graphics.Path path = new android.graphics.Path();
        path.moveTo(x, y);
        path.lineTo(x + 1, y + 1);
        builder.addStroke(new android.accessibilityservice.GestureDescription
            .StrokeDescription(path, 0, 1));
        dispatchGesture(builder.build(), null, null);
    }

    // ===== CURSOR CONTROL =====
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
