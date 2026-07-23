package com.gesture.assist;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

public class ShizukuInjectorService extends Service {
    private static final String TAG = "ShizukuInjector";
    private boolean ready = false;
    private UltimateOptimizer optimizer;
    private PowerManager.WakeLock wakeLock;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.gesture.assist.SWIPE".equals(intent.getAction())) {
                float x1 = intent.getFloatExtra("x1", 0);
                float y1 = intent.getFloatExtra("y1", 0);
                float x2 = intent.getFloatExtra("x2", 0);
                float y2 = intent.getFloatExtra("y2", 0);
                int duration = intent.getIntExtra("duration", 0);
                executeSwipe(x1, y1, x2, y2, duration);
            }
        }
    };

    private void executeSwipe(float x1, float y1, float x2, float y2, int duration) {
        if (!ready) return;
        new Thread(() -> {
            try {
                String cmd = String.format("input swipe %d %d %d %d %d",
                        (int) x1, (int) y1, (int) x2, (int) y2, Math.max(1, duration));
                ShizukuShell.runCommand(cmd);
                Log.d(TAG, "Swipe executed: " + cmd);
            } catch (Exception e) {
                Log.e(TAG, "Swipe error", e);
            }
        }).start();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        optimizer = new UltimateOptimizer(this);

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DuongChai:WakeLock");
        wakeLock.acquire(3600 * 1000L);

        if (Shizuku.pingBinder()) {
            if (Shizuku.checkSelfPermission() == 0) {
                ready = true;
                Toast.makeText(this, "Shizuku ready", Toast.LENGTH_SHORT).show();
                optimizer.optimizeAll();
            } else {
                Shizuku.requestPermission(1000);
            }
        } else {
            Toast.makeText(this, "Shizuku chưa chạy!", Toast.LENGTH_LONG).show();
        }

        registerReceiver(receiver, new IntentFilter("com.gesture.assist.SWIPE"));
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(receiver);
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
    }
}
