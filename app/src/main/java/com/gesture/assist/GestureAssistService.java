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
import android.provider.Settings;
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

    // ===== BUFF TỐI ĐA =====
    private float sensitivity = 100000.0f;
    private float acceleration = 5.0f;
    private int cursorSize = 1;

    private Handler handler = new Handler();
    private SharedPreferences prefs;
    private boolean isSuperTouchOn = true;
    private boolean isPointerSpeedOn = true;
    private boolean isDispatchOn = false;
    private int currentDensity = 120;

    // ===== CÁC TWEAK MỚI =====
    private boolean isGpuRenderOn = true;
    private boolean isHwOverlayOff = true;
    private boolean isAnimationOff = true;
    private boolean isPerformanceModeOn = true;
    private boolean isGameModeOn = true;
    private boolean isBackgroundLimitOn = true;

    private Random random = new Random();

    @Override
    public void onCreate() {
        super.onCreate();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        Point size = new Point();
        wm.getDefaultDisplay().getSize(size);
        screenWidth = size.x;
        screenHeight = size.y;

        prefs = getSharedPreferences("gamepad_settings", MODE_PRIVATE);
        loadSettings();

        if (Settings.canDrawOverlays(this)) {
            createOverlay();
            createCursorView();
        } else {
            Toast.makeText(this, "⚠️ Cần bật Overlay!", Toast.LENGTH_LONG).show();
        }

        // === ÁP DỤNG TOÀN BỘ TWEAK ===
        applyAllOptimizations();

        Toast.makeText(this, "🔥 Cuto Ultimate đã kích hoạt!", Toast.LENGTH_LONG).show();
    }

    private void loadSettings() {
        sensitivity = prefs.getInt("sensitivity", 20000);
        currentDensity = prefs.getInt("density", 120);
        isSuperTouchOn = prefs.getBoolean("super_touch", true);
        isPointerSpeedOn = prefs.getBoolean("pointer_speed", true);
        isDispatchOn = prefs.getBoolean("dispatch", false);
        isGpuRenderOn = prefs.getBoolean("gpu_render", true);
        isHwOverlayOff = prefs.getBoolean("hw_overlay_off", true);
        isAnimationOff = prefs.getBoolean("animation_off", true);
        isPerformanceModeOn = prefs.getBoolean("performance_mode", true);
        isGameModeOn = prefs.getBoolean("game_mode", true);
        isBackgroundLimitOn = prefs.getBoolean("background_limit", true);
    }

    // ===== ÁP DỤNG TOÀN BỘ TWEAK (KHÔNG ROOT) =====
    private void applyAllOptimizations() {
        new Thread(() -> {
            try {
                if (isSuperTouchOn) {
                    runCommand("setprop ro.min_pointer_dur 0");
                    runCommand("setprop debug.input.smoothing 0");
                    runCommand("setprop windowsmgr.max_events_per_sec 9999");
                    runCommand("setprop touch.pressure.scale 0.0");
                    runCommand("setprop touch.size.scale 0.0");
                    runCommand("setprop persist.sys.touch.sampling.rate 10000");
                    runCommand("setprop debug.touch.sensitivity 100000");
                }
                if (isPointerSpeedOn) {
                    runCommand("settings put system pointer_speed 7");
                }
                runCommand("wm density " + currentDensity);
                runCommand("wm scaling off");

                if (isGpuRenderOn) {
                    runCommand("settings put global force_gpu_rendering 1");
                    runCommand("settings put global debug.hwui.force_gpu_render true");
                }
                if (isHwOverlayOff) {
                    runCommand("settings put global disable_hw_overlays 1");
                }
                if (isAnimationOff) {
                    runCommand("settings put global window_animation_scale 0.0");
                    runCommand("settings put global transition_animation_scale 0.0");
                    runCommand("settings put global animator_duration_scale 0.0");
                }
                if (isPerformanceModeOn) {
                    runCommand("cmd power set-fixed-performance-mode-enabled true");
                }
                if (isGameModeOn) {
                    runCommand("cmd game mode performance com.dts.freefire");
                    runCommand("cmd game mode performance com.dts.freefiremax");
                }
                if (isBackgroundLimitOn) {
                    runCommand("settings put global background_process_limit 1");
                }
                runCommand("settings put system long_press_timeout 50");
                runCommand("settings put system scroll_friction 0.0");
                runCommand("cmd activity kill-all");
                runCommand("setprop debug.hwui.renderer skiavk");

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
            // Bỏ qua lỗi
        }
    }

    // ===== OVERLAY =====
    private void createOverlay() {
        if (overlay != null) return;
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

    // ===== CURSOR =====
    private void createCursorView() {
        if (cursorView != null) return;
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

    // ===== XỬ LÝ TOUCH (BẢN FULL) =====
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
            vibrate(5);
            return;
        }

        if (action == MotionEvent.ACTION_MOVE && isTrackpadActive) {
            float dx = (x - lastX) * sensitivity * acceleration;
            float dy = (y - lastY) * sensitivity * acceleration;

            // Thêm nhiễu ngẫu nhiên để giảm pattern bất thường
            float noiseX = (random.nextFloat() - 0.5f) * 0.3f;
            float noiseY = (random.nextFloat() - 0.5f) * 0.3f;
            dx += noiseX;
            dy += noiseY;

            float newX = Math.max(0, Math.min(screenWidth - cursorSize, cursorX + dx));
            float newY = Math.max(0, Math.min(screenHeight - cursorSize, cursorY + dy));

            // Nếu bật Dispatch Gesture, gửi cử chỉ ảo vào game
            if (isDispatchOn) {
                sendGamepadMove(cursorX + cursorSize/2, cursorY + cursorSize/2,
                                newX + cursorSize/2, newY + cursorSize/2);
            }

            cursorX = newX;
            cursorY = newY;
            moveCursor();
            lastX = x;
            lastY = y;

            // Delay ngẫu nhiên 5-10ms
            try {
                Thread.sleep(5 + random.nextInt(5));
            } catch (InterruptedException ignored) {}
            return;
        }

        if (action == MotionEvent.ACTION_UP && isTrackpadActive) {
            isTrackpadActive = false;
            if (isDispatchOn) {
                clickGamepad(cursorX + cursorSize/2, cursorY + cursorSize/2);
            }
            vibrate(8);
            hideCursor();
        }
    }

    // ===== DISPATCH GESTURE (GỬI CỬ CHỈ ẢO VÀO GAME) =====
    private void sendGamepadMove(float x1, float y1, float x2, float y2) {
        Path path = new Path();
        path.moveTo(x1, y1);
        path.lineTo(x2, y2);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 0));
        dispatchGesture(builder.build(), null, null);
    }

    private void clickGamepad(float x, float y) {
        Path path = new Path();
        path.moveTo(x, y);
        path.lineTo(x + 1, y + 1);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 0));
        dispatchGesture(builder.build(), null, null);
    }

    // ===== CURSOR CONTROL =====
    private void showCursor() {
        if (cursorView == null) return;
        isCursorVisible = true;
        cursorView.setVisibility(View.VISIBLE);
        cursorParams.x = (int) cursorX;
        cursorParams.y = (int) cursorY;
        wm.updateViewLayout(cursorView, cursorParams);
    }

    private void moveCursor() {
        if (cursorView == null || !isCursorVisible) return;
        cursorParams.x = (int) cursorX;
        cursorParams.y = (int) cursorY;
        wm.updateViewLayout(cursorView, cursorParams);
    }

    private void hideCursor() {
        if (cursorView == null) return;
        isCursorVisible = false;
        cursorView.setVisibility(View.GONE);
    }

    // ===== VIBRATE =====
    private void vibrate(int ms) {
        if (vibrator == null) return;
        if (Build.VERSION.SDK_INT >= 26) {
            vibrator.vibrate(VibrationEffect.createOneShot(ms, 20));
        } else {
            vibrator.vibrate(ms);
        }
    }

    // ===== LIFECYCLE =====
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

    // ===== NESTED CLASS OVERLAYVIEW =====
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
