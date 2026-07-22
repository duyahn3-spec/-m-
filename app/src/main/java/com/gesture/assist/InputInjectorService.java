package com.gesture.assist;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.util.Log;

import java.lang.reflect.Method;

import rikka.shizuku.Shizuku;

public class InputInjectorService extends Service {
    private static final String TAG = "InputInjector";
    private IBinder inputManagerBinder;
    private Method injectMethod;
    private boolean ready = false;

    @Override
    public void onCreate() {
        super.onCreate();
        if (Shizuku.pingBinder()) {
            try {
                // Lấy InputManager hệ thống qua Binder
                inputManagerBinder = Shizuku.getSystemService(Context.INPUT_SERVICE);
                if (inputManagerBinder != null) {
                    injectMethod = inputManagerBinder.getClass().getMethod(
                            "injectInputEvent", MotionEvent.class, int.class
                    );
                    ready = true;
                    Log.d(TAG, "InputInjector ready");
                }
            } catch (Exception e) {
                Log.e(TAG, "Init fail", e);
            }
        }
    }

    public boolean injectTouch(float x, float y, int action) {
        if (!ready || injectMethod == null) return false;
        try {
            long now = SystemClock.uptimeMillis();
            MotionEvent event = MotionEvent.obtain(now, now, action, x, y, 0);
            event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
            injectMethod.invoke(inputManagerBinder, event, 0);
            event.recycle();
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Inject failed", e);
            return false;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new IInputInjector.Stub() {
            @Override
            public boolean inject(float x, float y, int action) throws RemoteException {
                return injectTouch(x, y, action);
            }
        };
    }

    // AIDL stub định nghĩa đơn giản (dùng Binder thẳng)
    public static abstract class IInputInjector extends Binder implements IInputInjectorInterface {
        public static IInputInjector asInterface(IBinder obj) {
            // Không dùng AIDL thực, ta dùng Binder trực tiếp trong service
            // Tao sẽ truyền qua interface đơn giản
            return null;
        }
    }

    // Interface cho binder
    public interface IInputInjectorInterface {
        boolean inject(float x, float y, int action) throws RemoteException;
    }
}
