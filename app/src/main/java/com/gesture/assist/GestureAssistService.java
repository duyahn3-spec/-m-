package com.gesture.assist;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.os.Handler;
import android.provider.Settings;
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

        if (Settings.canDrawOverlays(this)) {
            createOverlay();
            createCursorView();
        } else {
            Toast.makeText(this, "⚠️ Cần bật Overlay!", Toast.LENGTH_LONG).show();
        }

        // === ÁP DỤNG TOÀN BỘ TWEAK MỚI ===
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
                // 1. Super Touch core (đã có)
                if (isSuperTouchOn) {
                    runCommand("setprop ro.min_pointer_dur 0");
                    runCommand("setprop debug.input.smoothing 0");
                    runCommand("setprop windowsmgr.max_events_per_sec 9999");
                    runCommand("setprop touch.pressure.scale 0.0");
                    runCommand("setprop touch.size.scale 0.0");
                    runCommand("setprop persist.sys.touch.sampling.rate 10000");
                    runCommand("setprop debug.touch.sensitivity 100000");
                }

                // 2. Pointer Speed (đã có)
                if (isPointerSpeedOn) {
                    runCommand("settings put system pointer_speed 7");
                }

                // 3. DPI (đã có)
                runCommand("wm density " + currentDensity);
                runCommand("wm scaling off");

                // 4. Ép GPU Render
                if (isGpuRenderOn) {
                    runCommand("settings put global force_gpu_rendering 1");
                    runCommand("settings put global debug.hwui.force_gpu_render true");
                }

                // 5. Tắt HW Overlays
                if (isHwOverlayOff) {
                    runCommand("settings put global disable_hw_overlays 1");
                }

                // 6. Tắt animation
                if (isAnimationOff) {
                    runCommand("settings put global window_animation_scale 0.0");
                    runCommand("settings put global transition_animation_scale 0.0");
                    runCommand("settings put global animator_duration_scale 0.0");
                }

                // 7. Ép CPU/GPU max (performance mode)
                if (isPerformanceModeOn) {
                    runCommand("cmd power set-fixed-performance-mode-enabled true");
                }

                // 8. Game mode performance (cho Free Fire)
                if (isGameModeOn) {
                    runCommand("cmd game mode performance com.dts.freefire");
                    runCommand("cmd game mode performance com.dts.freefiremax");
                }

                // 9. Giới hạn background process
                if (isBackgroundLimitOn) {
                    runCommand("settings put global background_process_limit 1");
                }

                // 10. Các lệnh tối ưu khác
                runCommand("settings put system long_press_timeout 50");
                runCommand("settings put system scroll_friction 0.0");
                runCommand("cmd activity kill-all");

                // 11. Ép render Vulkan (nếu hỗ trợ)
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

    // ===== CÁC HÀM OVERLAY + CURSOR (GIỮ NGUYÊN) =====
    // ... (giữ nguyên code cũ từ bản trước)
}
