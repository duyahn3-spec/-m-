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
            if ("com.gesture.assist.TOGGLE_ALL".equals(intent.getAction())) {
                boolean enable = intent.getBooleanExtra("enable", true);
                if (enable) {
                    executeEnable();
                } else {
                    executeDisable();
                }
            }
        }
    };

    private void executeEnable() {
        if (!ready) {
            handler.post(() -> Toast.makeText(this, "Shizuku chưa sẵn sàng", Toast.LENGTH_SHORT).show());
            return;
        }
        new Thread(() -> {
            try {
                String sizeOutput = runCommandAndGetOutput("wm size");
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
                handler.post(() -> Toast.makeText(ShizukuInjectorService.this, "✅ Kéo giãn 1.7x", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                Log.e(TAG, "Error", e);
            }
        }).start();
    }

    private void executeDisable() {
        if (!ready) return;
        new Thread(() -> {
            try {
                runCommand("wm size reset");
                handler.post(() -> Toast.makeText(ShizukuInjectorService.this, "🔁 Reset màn hình", Toast.LENGTH_SHORT).show());
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

        if (Shizuku.pingBinder()) {
            if (Shizuku.checkSelfPermission() == 0) {
                ready = true;
                Toast.makeText(this, "Shizuku ready", Toast.LENGTH_SHORT).show();
            } else {
                Shizuku.requestPermission(1000);
                Toast.makeText(this, "Đang xin quyền Shizuku...", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Shizuku chưa chạy!", Toast.LENGTH_LONG).show();
        }

        registerReceiver(receiver, new IntentFilter("com.gesture.assist.TOGGLE_ALL"));
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
