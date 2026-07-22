package com.gesture.assist;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

public class ShizukuInjectorService extends Service {
    private static final String TAG = "ShizukuInjector";
    private boolean ready = false;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.gesture.assist.SWIPE".equals(intent.getAction())) {
                float x1 = intent.getFloatExtra("x1", 0);
                float y1 = intent.getFloatExtra("y1", 0);
                float x2 = intent.getFloatExtra("x2", 0);
                float y2 = intent.getFloatExtra("y2", 0);
                int duration = intent.getIntExtra("duration", 1);
                executeSwipe(x1, y1, x2, y2, duration);
            }
        }
    };

    private void executeSwipe(float x1, float y1, float x2, float y2, int duration) {
        if (!ready) {
            Log.e(TAG, "Shizuku not ready");
            return;
        }

        new Thread(() -> {
            try {
                String cmd = String.format("input swipe %d %d %d %d %d",
                        (int) x1, (int) y1, (int) x2, (int) y2, duration);
                // Dùng Runtime.exec() thay vì Shizuku.newProcess()
                Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
                process.waitFor();
                Log.d(TAG, "Swipe executed: " + cmd);
            } catch (Exception e) {
                Log.e(TAG, "Swipe error", e);
            }
        }).start();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        if (Shizuku.pingBinder()) {
            if (Shizuku.checkSelfPermission() == 0) {
                ready = true;
                Toast.makeText(this, "Shizuku ready (Runtime.exec mode)", Toast.LENGTH_SHORT).show();
            } else {
                Shizuku.requestPermission(1000);
                Toast.makeText(this, "Đang xin quyền Shizuku...", Toast.LENGTH_SHORT).show();
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
        super.onDestroy();
    }
}
