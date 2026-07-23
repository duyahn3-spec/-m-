package com.gesture.assist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent serviceIntent = new Intent(context, ShizukuInjectorService.class);
            context.startForegroundService(serviceIntent);
            Toast.makeText(context, "🚀 Duong Chai Dim OK đã khởi động cùng máy!", Toast.LENGTH_LONG).show();
        }
    }
}
