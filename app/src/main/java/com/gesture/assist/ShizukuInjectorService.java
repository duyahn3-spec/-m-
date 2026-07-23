package com.gesture.assist;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import rikka.shizuku.Shizuku;

public class ShizukuInjectorService extends Service {
    private static final String TAG = "ShizukuInjector";
    private boolean ready = false;
    private UltimateOptimizer optimizer;
    private PowerManager.WakeLock wakeLock;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "shizuku_channel";

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.gesture.assist.SWIPE".equals(intent.getAction())) {
                float x1 = intent.getFloatExtra("x1", 0);
                float y1 = intent.getFloatExtra("y1", 0);
                float x2 = intent.getFloatExtra("x2", 0);
                float y2 = intent.getFloatExtra("y2", 0);
                int duration = intent.getIntExtra("duration", 0);
                executor.submit(() -> executeSwipe(x1, y1, x2, y2, duration));
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        // Tạo notification channel và gọi startForeground ngay lập tức
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification());

        optimizer = new UltimateOptimizer(this);

        // WakeLock với try-finally
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "DuongChai:WakeLock");
        wakeLock.acquire(10 * 60 * 1000L); // 10 phút

        // Kiểm tra Shizuku và xin quyền
        if (Shizuku.pingBinder()) {
            if (Shizuku.checkSelfPermission() == 0) {
                ready = true;
                Toast.makeText(this, "Shizuku ready", Toast.LENGTH_SHORT).show();
                optimizer.optimizeAll();
            } else {
                Shizuku.requestPermission(1000);
                // Lắng nghe kết quả xin quyền
                Shizuku.addRequestPermissionResultListener((requestCode, grantResult) -> {
                    if (requestCode == 1000 && grantResult == 0) {
                        ready = true;
                        Toast.makeText(this, "Shizuku permission granted", Toast.LENGTH_SHORT).show();
                        optimizer.optimizeAll();
                    }
                });
            }
        } else {
            Toast.makeText(this, "Shizuku chưa chạy!", Toast.LENGTH_LONG).show();
        }

        // Đăng ký receiver với flag an toàn
        IntentFilter filter = new IntentFilter("com.gesture.assist.SWIPE");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(receiver, filter);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Shizuku Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        Intent intent = new Intent(this, GestureAssistActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Duong Chai Shizuku")
                .setContentText("Service đang chạy...")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .setOngoing(true)
                .setContentIntent(pendingIntent)
                .build();
    }

    private void executeSwipe(float x1, float y1, float x2, float y2, int duration) {
        if (!ready) return;
        try {
            String cmd = String.format("input swipe %d %d %d %d %d",
                    (int) x1, (int) y1, (int) x2, (int) y2, Math.max(1, duration));
            ShizukuShell.runCommand(cmd);
            Log.d(TAG, "Swipe executed: " + cmd);
        } catch (Exception e) {
            Log.e(TAG, "Swipe error", e);
        }
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
        executor.shutdownNow();
        super.onDestroy();
    }
}
