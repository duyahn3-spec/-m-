package com.gesture.assist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Khởi động service để áp dụng tweak
            Intent serviceIntent = new Intent(context, GestureAssistService.class);
            context.startService(serviceIntent);
            Toast.makeText(context, "🔥 Cuto Ultimate đã khởi động cùng máy!", Toast.LENGTH_LONG).show();
        }
    }
}
