package com.gesture.assist;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UltimateOptimizer {
    private static final String TAG = "UltimateOptimizer";
    private Context context;
    private Handler handler = new Handler(Looper.getMainLooper());

    public UltimateOptimizer(Context context) {
        this.context = context;
    }

    public void optimizeAll() {
        new Thread(() -> {
            try {
                // Animation
                ShizukuShell.runCommand("settings put system window_animation_scale 0.0");
                ShizukuShell.runCommand("settings put system transition_animation_scale 0.0");
                ShizukuShell.runCommand("settings put system animator_duration_scale 0.0");

                // GPU render
                ShizukuShell.runCommand("settings put global force_gpu_rendering 1");

                // Pointer speed
                ShizukuShell.runCommand("settings put system pointer_speed 50");

                // Kill background (nếu hỗ trợ)
                ShizukuShell.runCommand("cmd activity kill-all");

                // Kéo giãn màn hình 1.7x – parse đúng
                String sizeOutput = ShizukuShell.runCommand("wm size");
                if (sizeOutput != null && !sizeOutput.isEmpty()) {
                    // Tìm số trong output
                    Pattern pattern = Pattern.compile("(\\d+)x(\\d+)");
                    Matcher matcher = pattern.matcher(sizeOutput);
                    if (matcher.find()) {
                        try {
                            int width = Integer.parseInt(matcher.group(1));
                            int height = Integer.parseInt(matcher.group(2));
                            int newWidth = (int) (width * 1.7);
                            int newHeight = (int) (height * 1.7);
                            ShizukuShell.runCommand("wm size " + newWidth + "x" + newHeight);
                            Log.d(TAG, "Kéo giãn thành công: " + newWidth + "x" + newHeight);
                        } catch (NumberFormatException e) {
                            Log.e(TAG, "Parse lỗi", e);
                        }
                    }
                }

                handler.post(() -> Toast.makeText(context, "✅ Tối ưu thành công!", Toast.LENGTH_LONG).show());

            } catch (Exception e) {
                Log.e(TAG, "Optimize error", e);
                handler.post(() -> Toast.makeText(context, "❌ Lỗi tối ưu: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
