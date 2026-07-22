package com.gesture.assist;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;
import android.os.Handler;
import android.os.Looper;

import rikka.shizuku.Shizuku;
import rikka.shizuku.SystemServiceHelper;

public class ShizukuInjectorService extends Service {
    private static final String TAG = "ShizukuInjector";
    private boolean ready = false;
    private Handler handler;
    private IBinder shellBinder;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.gesture.assist.EXECUTE".equals(intent.getAction())) {
                executeAllCommands();
            }
        }
    };

    private void executeAllCommands() {
        if (!ready || shellBinder == null) {
            Toast.makeText(this, "Shizuku chưa sẵn sàng", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                // Dùng SystemServiceHelper để chạy lệnh shell qua Shizuku
                runShellCommand("settings put system pointer_speed 7");
                runShellCommand("settings put system window_animation_scale 0.3");
                runShellCommand("settings put system transition_animation_scale 0.3");
                runShellCommand("settings put system animator_duration_scale 0.3");
                runShellCommand("setprop debug.input.smoothing 0.3");
                runShellCommand("setprop debug.sf.max_frame_latency 0");
                runShellCommand("setprop debug.hwui.target_gpu_time_percent 300");
                runShellCommand("setprop debug.hwui.renderer opengl");
                runShellCommand("setprop debug.hwui.force_gpu 1");
                runShellCommand("cmd activity kill-all");
                runShellCommand("cmd power set-fixed-performance-mode-enabled true");

                // Lấy và set resolution
                String sizeOutput = runShellCommandAndGetOutput("wm size");
                if (sizeOutput != null && sizeOutput.contains("x")) {
                    String[] parts = sizeOutput.trim().split("x");
                    if (parts.length == 2) {
                        int width = Integer.parseInt(parts[0].replaceAll("\\D", ""));
                        int height = Integer.parseInt(parts[1].replaceAll("\\D", ""));
                        int newWidth = (int) (width * 1.6);
                        int newHeight = (int) (height * 1.6);
                        runShellCommand("wm size " + newWidth + "x" + newHeight);
                    }
                }

                String notifyCmd = "cmd notification post -t '🚀 ' 'CÁI ĐÙ CÂU LÁP BỰ BÁ SÀN CỦA MÀY ĐÂY!' 'AIMLOCK 🔥💥 | ĐỘ NHẠY X2 TRIỆU TỐC ĐỘ KÉO PHÁT LÊN TRỜI💯| CÀI VÀO MÁY LAG NHƯ LON BẮN ĐÉO CÓ TRÌNH SỦA CON CAK | X1000000000 TỶ ĐỘ SUPPER MAX ĐẸP TRAI CỦA HẢI DƯƠNG CÒN LẠI TỤI BÂY ĐÉO CÓ CẢNH| TAO BÁ SÀN NHẤT ĐÈO MẸ BỌN NGUUUU LÒN ÓC CẶT TUỔI LỒN NẰM XUỐNG MẤY CON CHÓ 😏| BỌN MÀY LÁP NHƯ QUẢ ỚT 🌶️ CÀI VÀO NHƯ KHÔNG CHỈ DÀNH CHO TAO LÁP BỰ MỚI CÓ TÁC DỤNG😎| Hai Dương 🗿!'";
                runShellCommand(notifyCmd);

                handler.post(() -> Toast.makeText(ShizukuInjectorService.this, "✅ DONE!", Toast.LENGTH_SHORT).show());

            } catch (Exception e) {
                Log.e(TAG, "Error executing commands", e);
                handler.post(() -> Toast.makeText(ShizukuInjectorService.this, "❌ Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void runShellCommand(String cmd) {
        try {
            SystemServiceHelper.getShell().run(cmd);
            Log.d(TAG, "Executed: " + cmd);
        } catch (RemoteException | InterruptedException e) {
            Log.e(TAG, "Failed: " + cmd, e);
        }
    }

    private String runShellCommandAndGetOutput(String cmd) {
        try {
            return SystemServiceHelper.getShell().runAndGetOutput(cmd);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get output: " + cmd, e);
            return null;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());

        if (Shizuku.pingBinder()) {
            if (Shizuku.checkSelfPermission() == 0) {
                ready = true;
                try {
                    shellBinder = Shizuku.getSystemService("shell");
                } catch (Exception e) {
                    Log.e(TAG, "Cannot get shell service", e);
                }
                Toast.makeText(this, "Shizuku sẵn sàng", Toast.LENGTH_SHORT).show();
            } else {
                Shizuku.requestPermission(1000);
                Toast.makeText(this, "Đang xin quyền Shizuku...", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Shizuku chưa chạy!", Toast.LENGTH_LONG).show();
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
