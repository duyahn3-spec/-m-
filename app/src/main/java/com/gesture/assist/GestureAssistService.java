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
                executeAllCommands();
            }
        }
    };

    private void executeAllCommands() {
        if (!ready) {
            Toast.makeText(this, "Shizuku not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                // 1. Increase pointer speed
                runCommand("settings put system pointer_speed 7");

                // 2. Reduce animation scale
                runCommand("settings put system window_animation_scale 0.3");
                runCommand("settings put system transition_animation_scale 0.3");
                runCommand("settings put system animator_duration_scale 0.3");

                // 3. Disable touch smoothing
                runCommand("setprop debug.input.smoothing 0.3");

                // 4. Reduce frame latency
                runCommand("setprop debug.sf.max_frame_latency 0");

                // 5. Increase GPU target time (ép GPU làm việc nhiều hơn)
                runCommand("setprop debug.hwui.target_gpu_time_percent 300");

                // 6. Ép hệ thống dùng GPU render (giảm tải CPU)
                runCommand("setprop debug.hwui.renderer opengl");
                runCommand("setprop debug.hwui.force_gpu 1");

                // 7. Kill background apps
                runCommand("cmd activity kill-all");

                // 8. Enable fixed performance mode (if supported)
                runCommand("cmd power set-fixed-performance-mode-enabled true");

                // 9. Scale screen resolution to 1.6x (1250p approximation)
                String sizeCmd = "wm size";
                String sizeOutput = runCommandAndGetOutput(sizeCmd);
                if (sizeOutput != null && sizeOutput.contains("x")) {
                    String[] parts = sizeOutput.trim().split("x");
                    if (parts.length == 2) {
                        int width = Integer.parseInt(parts[0].replaceAll("\\D", ""));
                        int height = Integer.parseInt(parts[1].replaceAll("\\D", ""));
                        int newWidth = (int) (width * 1.6);
                        int newHeight = (int) (height * 1.6);
                        runCommand("wm size " + newWidth + "x" + newHeight);
                    }
                }

                // 10. Send notification
                String notifyCmd = "cmd notification post -t '🚀 ' 'CÁI ĐÙ CÂU LÁP BỰ BÁ SÀN CỦA MÀY ĐÂY!' 'AIMLOCK 🔥💥 | ĐỘ NHẠY X2 TRIỆU TỐC ĐỘ KÉO PHÁT LÊN TRỜI💯| CÀI VÀO MÁY LAG NHƯ LON BẮN ĐÉO CÓ TRÌNH SỦA CON CAK | X1000000000 TỶ ĐỘ SUPPER MAX ĐẸP TRAI CỦA HẢI DƯƠNG CÒN LẠI TỤI BÂY ĐÉO CÓ CẢNH| TAO BÁ SÀN NHẤT ĐÈO MẸ BỌN NGUUUU LÒN ÓC CẶT TUỔI LỒN NẰM XUỐNG MẤY CON CHÓ 😏| BỌN MÀY LÁP NHƯ QUẢ ỚT 🌶️ CÀI VÀO NHƯ KHÔNG CHỈ DÀNH CHO TAO LÁP BỰ MỚI CÓ TÁC DỤNG😎| Hai Dương 🗿!'";
                runCommand(notifyCmd);

                // 11. Done
                handler.post(() -> Toast.makeText(this, "DONE!", Toast.LENGTH_SHORT).show());

            } catch (Exception e) {
                Log.e(TAG, "Error executing commands", e);
                handler.post(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void runCommand(String cmd) {
        try {
            Shizuku.newProcess(new String[]{"sh", "-c", cmd}, null, null).waitFor();
            Log.d(TAG, "Command executed: " + cmd);
        } catch (Exception e) {
            Log.e(TAG, "Command failed: " + cmd, e);
        }
    }

    private String runCommandAndGetOutput(String cmd) {
        try {
            Process process = Shizuku.newProcess(new String[]{"sh", "-c", cmd}, null, null);
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(process.getInputStream()));
                return reader.readLine();
            }
        } catch (Exception e) {
            Log.e(TAG, "Command output failed: " + cmd, e);
        }
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());

        if (Shizuku.pingBinder()) {
            if (Shizuku.checkSelfPermission() == 0) {
                ready = true;
                Log.d(TAG, "Shizuku ready");
                Toast.makeText(this, "Shizuku ready", Toast.LENGTH_SHORT).show();
            } else {
                Shizuku.requestPermission(1000);
            }
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
