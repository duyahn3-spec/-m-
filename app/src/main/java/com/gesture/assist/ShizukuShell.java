package com.gesture.assist;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import java.lang.reflect.Method;

import rikka.shizuku.Shizuku;

public class ShizukuShell {
    private static final String TAG = "ShizukuShell";
    private static Object shellService;
    private static Method runMethod;

    static {
        try {
            IBinder binder = Shizuku.getSystemService("shell");
            if (binder != null) {
                // Lấy IShellService.Stub.asInterface(binder) qua reflection
                Class<?> stubClass = Class.forName("rikka.shizuku.shell.IShellService$Stub");
                Method asInterface = stubClass.getMethod("asInterface", IBinder.class);
                shellService = asInterface.invoke(null, binder);
                // Lấy method run(String cmd)
                runMethod = shellService.getClass().getMethod("run", String.class);
            }
        } catch (Exception e) {
            Log.e(TAG, "Không thể khởi tạo ShellService", e);
        }
    }

    public static String runCommand(String cmd) {
        if (shellService == null || runMethod == null) {
            return "ERROR: ShellService not available";
        }
        try {
            // Gọi run(cmd) và trả về output (nếu có)
            Object result = runMethod.invoke(shellService, cmd);
            return result != null ? result.toString() : "Done (no output)";
        } catch (Exception e) {
            Log.e(TAG, "Lỗi khi chạy lệnh: " + cmd, e);
            return "ERROR: " + e.getMessage();
        }
    }
}
