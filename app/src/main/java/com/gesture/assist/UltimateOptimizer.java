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
            try {
                // === CPU (có thể không có tác dụng nếu kernel không cho) ===
                ShizukuShell.runCommand("echo performance > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor");
                ShizukuShell.runCommand("echo performance > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor");

                // === I/O ===
                ShizukuShell.runCommand("echo cfq > /sys/block/mmcblk0/queue/scheduler");
                ShizukuShell.runCommand("echo 2048 > /sys/block/mmcblk0/queue/read_ahead_kb");

                // === VM ===
                ShizukuShell.runCommand("echo 10 > /proc/sys/vm/swappiness");
                ShizukuShell.runCommand("echo 50 > /proc/sys/vm/dirty_ratio");

                // === Animation (có tác dụng) ===
                ShizukuShell.runCommand("settings put system window_animation_scale 0.0");
                ShizukuShell.runCommand("settings put system transition_animation_scale 0.0");
                ShizukuShell.runCommand("settings put system animator_duration_scale 0.0");

                // === GPU Render (có tác dụng) ===
                ShizukuShell.runCommand("settings put global force_gpu_rendering 1");
                ShizukuShell.runCommand("setprop debug.hwui.renderer opengl");
                ShizukuShell.runCommand("setprop debug.hwui.force_gpu 1");

                // === Kill background ===
                ShizukuShell.runCommand("cmd activity kill-all");

                // === Pointer speed ===
                ShizukuShell.runCommand("settings put system pointer_speed 50");

                handler.post(() -> Toast.makeText(context, "✅ Tối ưu thành công!", Toast.LENGTH_LONG).show());

            } catch (Exception e) {
                handler.post(() -> Toast.makeText(context, "❌ Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}
