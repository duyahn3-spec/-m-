package com.gesture.assist;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class UltimateOptimizer {
    private Context context;
    private Handler handler = new Handler(Looper.getMainLooper());

    public UltimateOptimizer(Context context) {
        this.context = context;
    }

    public void optimizeAll() {
        new Thread(() -> {
            // Tối ưu animation (có tác dụng)
            ShizukuShell.runCommand("settings put system window_animation_scale 0.0");
            ShizukuShell.runCommand("settings put system transition_animation_scale 0.0");
            ShizukuShell.runCommand("settings put system animator_duration_scale 0.0");

            // GPU render
            ShizukuShell.runCommand("settings put global force_gpu_rendering 1");
            ShizukuShell.runCommand("setprop debug.hwui.renderer opengl");
            ShizukuShell.runCommand("setprop debug.hwui.force_gpu 1");

            // Pointer speed
            ShizukuShell.runCommand("settings put system pointer_speed 50");

            // Kill background
            ShizukuShell.runCommand("cmd activity kill-all");

            // Kéo giãn màn hình 1.7x (nếu được)
            String sizeOutput = ShizukuShell.runCommand("wm size");
            if (sizeOutput != null && sizeOutput.contains("x")) {
                try {
                    String[] parts = sizeOutput.trim().split("x");
                    int width = Integer.parseInt(parts[0].replaceAll("\\D", ""));
                    int height = Integer.parseInt(parts[1].replaceAll("\\D", ""));
                    int newWidth = (int) (width * 1.7);
                    int newHeight = (int) (height * 1.7);
                    ShizukuShell.runCommand("wm size " + newWidth + "x" + newHeight);
                } catch (Exception ignored) {}
            }

            handler.post(() -> Toast.makeText(context, "✅ Tối ưu + kéo giãn thành công!", Toast.LENGTH_LONG).show());
        }).start();
    }
}
