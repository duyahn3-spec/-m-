package com.gesture.assist;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;

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
                // === CPU ===
                runCommand("echo performance > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor");
                runCommand("echo performance > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor");
                runCommand("echo 1000000 > /sys/devices/system/cpu/cpu0/cpufreq/scaling_min_freq");
                runCommand("echo 1000000 > /sys/devices/system/cpu/cpu4/cpufreq/scaling_min_freq");

                // === GPU ===
                runCommand("echo performance > /sys/class/kgsl/kgsl-3d0/devfreq/governor");
                runCommand("echo 1000000000 > /sys/class/kgsl/kgsl-3d0/max_gpuclk");

                // === Giữ tất cả core online ===
                for (int i = 0; i < 8; i++) {
                    runCommand("echo 1 > /sys/devices/system/cpu/cpu" + i + "/online");
                }

                // === I/O Scheduler & Read-ahead ===
                runCommand("echo cfq > /sys/block/mmcblk0/queue/scheduler");
                runCommand("echo 2048 > /sys/block/mmcblk0/queue/read_ahead_kb");

                // === VM ===
                runCommand("echo 10 > /proc/sys/vm/swappiness");
                runCommand("echo 50 > /proc/sys/vm/dirty_ratio");
                runCommand("echo 30 > /proc/sys/vm/dirty_background_ratio");
                runCommand("echo 1 > /proc/sys/vm/overcommit_memory");

                // === Animation ===
                runCommand("settings put system window_animation_scale 0.0");
                runCommand("settings put system transition_animation_scale 0.0");
                runCommand("settings put system animator_duration_scale 0.0");

                // === GPU Render ===
                runCommand("settings put global force_gpu_rendering 1");
                runCommand("setprop debug.hwui.renderer opengl");
                runCommand("setprop debug.hwui.force_gpu 1");

                // === Kill background ===
                runCommand("cmd activity kill-all");

                // === Performance mode ===
                runCommand("cmd power set-fixed-performance-mode-enabled true");

                // === Tăng priority ===
                runCommand("echo -n 0 > /proc/sys/kernel/panic_on_oops");
                runCommand("echo -n 0 > /proc/sys/kernel/panic");

                // === TCP ===
                runCommand("echo 1 > /proc/sys/net/ipv4/tcp_low_latency");

                // === Limit background ===
                runCommand("settings put global background_process_limit 1");

                // === Mount noatime ===
                runCommand("mount -o remount,noatime /data");
                runCommand("mount -o remount,noatime /system");

                handler.post(() -> Toast.makeText(context, "✅ Tối ưu toàn bộ thành công!", Toast.LENGTH_LONG).show());

            } catch (Exception e) {
                Log.e(TAG, "Optimize error", e);
                handler.post(() -> Toast.makeText(context, "❌ Lỗi tối ưu: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void runCommand(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            process.waitFor();
            Log.d(TAG, "Executed: " + cmd);
        } catch (Exception e) {
            Log.e(TAG, "Failed: " + cmd, e);
        }
    }

    private String runCommandAndGetOutput(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            process.waitFor();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return reader.readLine();
        } catch (Exception e) {
            return null;
        }
    }
}
