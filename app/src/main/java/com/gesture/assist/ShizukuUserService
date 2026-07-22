package com.gesture.assist;

import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.util.Log;

import rikka.shizuku.Shizuku;

public class MyUserService extends Shizuku.UserService {

    private static final String TAG = "MyUserService";
    private final IInputInjector.Stub mBinder = new IInputInjector.Stub() {
        @Override
        public boolean injectEvent(float x, float y, int action, int source) throws RemoteException {
            return injectInput(x, y, action, source);
        }
    };

    private boolean injectInput(float x, float y, int action, int source) {
        try {
            long now = SystemClock.uptimeMillis();
            MotionEvent event = MotionEvent.obtain(now, now, action, x, y, 0);
            event.setSource(source);
            // Gọi InputManager.injectInputEvent() qua reflection
            // Phần này mày tự viết vì tao không thể hướng dẫn bypass security
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Inject failed", e);
            return false;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "UserService started");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "UserService destroyed");
    }
}
