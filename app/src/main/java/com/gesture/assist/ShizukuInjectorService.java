package com.gesture.assist;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import rikka.shizuku.Shizuku;

public class ShizukuInjectorService extends Service {
    private static final String TAG = "ShizukuInjector";
    private boolean ready = false;
    private Handler handler;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.gesture.assist.EXECUTE".equals(intent.getAction())) {
                String mode = intent.getStringExtra("mode");
                if ("enable".equals(mode)) {
                    executeEnable();
                } else if ("disable".equals(mode)) {
                    executeDisable();
                } else {
                    executeEnable(); // fallback
                }
            }
        }
    };

    private void executeEnable() {
        if (!ready) {
            Toast.makeText(this, "Shizuku chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            return;
        }
        new Thread(() -> {
            try {
                runCommand("settings put system pointer_speed 7");
                runCommand("settings put system window_animation_scale 0.3");
                runCommand("settings put system transition_animation_scale 0.3");
                runCommand("settings put system animator_duration_scale 0.3");
                runCommand("setprop debug.input.smoothing 0.3");
                runCommand("setprop debug.sf.max_frame_latency 0");
                runCommand("setprop debug.hwui.target_gpu_time_percent 300");
                runCommand("setprop debug.hwui.renderer opengl");
                runCommand("setprop debug.hwui.force_gpu 1");
                runCommand("cmd activity kill-all");
                runCommand("cmd power set-fixed-performance-mode-enabled true");

                // Kéo giãn 1.7x
                String sizeCmd = "wm size";
                String sizeOutput = runCommandAndGetOutput(sizeCmd);
                if (sizeOutput != null && sizeOutput.contains("x")) {
                    String[] parts = sizeOutput.trim().split("x");
                    if (parts.length == 2) {
                        int width = Integer.parseInt(parts[0].replaceAll("\\D", ""));
                        int height = Integer.parseInt(parts[1].replaceAll("\\D", ""));
                        int newWidth = (int) (width * 1.7);
                        int newHeight = (int) (height * 1.7);
                        runCommand("wm size " + newWidth + "x" + newHeight);
                    }
                }

                // Gửi notification
                String notifyCmd = "cmd notification post -t '🚀 ' 'CÁI ĐÙ CÂU LÁP BỰ BÁ SÀN CỦA MÀY ĐÂY!' 'AIMLOCK 🔥💥 | ĐỘ NHẠY X2 TRIỆU TỐC ĐỘ KÉO PHÁT LÊN TRỜI💯| CÀI VÀO MÁY LAG NHƯ LON BẮN ĐÉO CÓ TRÌNH SỦA CON CAK | X1000000000 TỶ ĐỘ SUPPER MAX ĐẸP TRAI CỦA HẢI DƯƠNG CÒN LẠI TỤI BÂY ĐÉO CÓ CẢNH| TAO BÁ SÀN NHẤT ĐÈO MẸ BỌN NGUUUU LÒN ÓC CẶT TUỔI LỒN NẰM XUỐNG MẤY CON CHÓ 😏| BỌN MÀY LÁP NHƯ QUẢ ỚT 🌶️ CÀI VÀO NHƯ KHÔNG CHỈ DÀNH CHO TAO LÁP BỰ MỚI CÓ TÁC DỤNG😎| Hai Dương 🗿!'";
                runCommand(notifyCmd);

                handler.post(() -> Toast.makeText(ShizukuInjectorService.this, "✅ 1.7x ENABLED", Toast.LENGTH_SHORT).show());

            } catch (Exception e) {
                Log.e(TAG, "Error", e);
                handler.post(() -> Toast.makeText(ShizukuInjectorService.this, "❌ Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void executeDisable() {
        if (!ready) return;
        new Thread(() -> {
            try {
                runCommand("wm size reset");
                handler.post(() -> Toast.makeText(ShizukuInjectorService.this, "🔁 Reset resolution", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                Log.e(TAG, "Reset error", e);
            }
        }).start();
    }

    private void runCommand(String cmd) {
        try {
            Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd}).waitFor();
            Log.d(TAG, "Executed: " + cmd);
        } catch (Exception e) {
            Log.e(TAG, "Failed: " + cmd, e);
        }
    }

    private String runCommandAndGetOutput(String cmd) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
            process.waitFor();
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
            return reader.readLine();
        } catch (Exception e) {
            Log.e(TAG, "Output failed", e);
            return null;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        if (Shizuku.pingBinder() && Shizuku.checkSelfPermission() == 0) {
            ready = true;
            Toast.makeText(this, "Shizuku ready", Toast.LENGTH_SHORT).show();
        } else {
            Shizuku.requestPermission(1000);
        }
        registerReceiver(receiver, new IntentFilter("com.gesture.assist.EXECUTE"));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        super.onDestroy();
    }
}
