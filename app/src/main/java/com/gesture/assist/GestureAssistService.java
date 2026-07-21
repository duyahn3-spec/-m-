package com.gesture.assist;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.widget.Toast;

import java.lang.reflect.Method;
import java.security.SecureRandom;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import rikka.shizuku.Shizuku;
import rikka.shizuku.SystemServiceHelper;

public class GestureAssistService extends AccessibilityService {

    private static final String TAG = "GestureService";
    private static final float SCALE_FACTOR = 1440.0f;

    private WindowManager wm;
    private OverlayView overlay;
    private ScheduledExecutorService scheduler;
    private AtomicBoolean isActive = new AtomicBoolean(false);
    private Vibrator vibrator;
    private float screenWidth, screenHeight;
    private float lastX, lastY;
    private SecureRandom random;

    private IBinder inputManagerBinder;
    private Method injectMethod;

    // === TỰ XIN QUYỀN SHIZUKU ===
    private void requestShizukuPermission() {
        if (Shizuku.pingBinder()) {
            if (!Shizuku.isPermissionGranted()) {
                Shizuku.requestPermission(0);
                Toast.makeText(this, "Đang yêu cầu quyền Shizuku...", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Shizuku chưa chạy! Mở Shizuku trước.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onAccessibilityEvent(android.view.accessibility.AccessibilityEvent event) {}

    @Override
    public void onInterrupt() { deactivate(); }

    @Override
    public void onCreate() {
        super.onCreate();
        scheduler = Executors.newSingleThreadScheduledExecutor();
        random = new SecureRandom();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);

        android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(metrics);
        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;

        // === TỰ XIN QUYỀN SHIZUKU ===
        requestShizukuPermission();

        if (Shizuku.pingBinder() && Shizuku.isPermissionGranted()) {
            try {
                inputManagerBinder = SystemServiceHelper.getSystemService("input");
                if (inputManagerBinder != null) {
                    injectMethod = inputManagerBinder.getClass().getMethod(
                        "injectInputEvent", MotionEvent.class, int.class
                    );
                    Toast.makeText(this, "✅ Shizuku + InputManager OK", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Shizuku error", e);
                Toast.makeText(this, "❌ Lỗi Shizuku: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "⚠️ Chưa có quyền Shizuku. Hãy bật trong Shizuku.", Toast.LENGTH_LONG).show();
        }

        createOverlay();
        Toast.makeText(this, "🔥 1440x + Shizuku", Toast.LENGTH_SHORT).show();
        activate();
    }

    // ... các hàm còn lại giữ nguyên (createOverlay, processTouch, injectTouch...)
            }
