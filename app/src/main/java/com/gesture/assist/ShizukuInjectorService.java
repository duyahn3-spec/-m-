package com.gesture.assist;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Method;

import rikka.shizuku.Shizuku;

public class ShizukuInjectorService extends Service {

    private static final String TAG = "ShizukuInjector";
    private Object inputManager;
    private Method injectMethod;
    private boolean ready = false;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.gesture.assist.INJECT".equals(intent.getAction())) {
                float x = intent.getFloatExtra("x", 0);
                float y = intent.getFloatExtra("y", 0);
                int action = intent.getIntExtra("action", MotionEvent.ACTION_DOWN);
                injectTouch(x, y, action);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();

        // Kiểm tra Shizuku đã chạy và có quyền chưa
        if (Shizuku.pingBinder()) {
            try {
                // Lấy InputManager hệ thống qua reflection
                Class<?> inputManagerClass = Class.forName("android.hardware.input.InputManager");
                Method getInstance = inputManagerClass.getMethod("getInstance");
                inputManager = getInstance.invoke(null);

                if (inputManager != null) {
                    injectMethod = inputManager.getClass().getMethod(
                            "injectInputEvent", MotionEvent.class, int.class
                    );
                    ready = true;
                    Toast.makeText(this, "✅ InputManager ready", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Log.e(TAG, "Init error", e);
                Toast.makeText(this, "❌ Lỗi init: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "❌ Shizuku chưa chạy!", Toast.LENGTH_LONG).show();
        }

        registerReceiver(receiver, new IntentFilter("com.gesture.assist.INJECT"));
    }

    private void injectTouch(float x, float y, int action) {
        if (!ready || injectMethod == null) return;

        try {
            long now = SystemClock.uptimeMillis();
            MotionEvent event = MotionEvent.obtain(now, now, action, x, y, 0);
            event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            injectMethod.invoke(inputManager, event, 0);
            event.recycle();
        } catch (Exception e) {
            Log.e(TAG, "Inject fail", e);
        }
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
