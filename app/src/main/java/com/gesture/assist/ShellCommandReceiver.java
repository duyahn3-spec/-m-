package com.gesture.assist;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.EditText;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

public class ShellCommandReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("📟 Nhập lệnh shell (qua Shizuku)");

        final EditText input = new EditText(context);
        input.setHint("vd: settings put system pointer_speed 20");
        builder.setView(input);

        builder.setPositiveButton("▶ Chạy", (dialog, which) -> {
            String cmd = input.getText().toString();
            if (!cmd.isEmpty()) {
                executeShell(cmd, context);
            }
        });
        builder.setNegativeButton("❌ Hủy", null);
        builder.show();
    }

    private void executeShell(String cmd, Context context) {
        Toast.makeText(context, "⏳ Đang chạy: " + cmd, Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            try {
                Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
                process.waitFor();
                Toast.makeText(context, "✅ Lệnh đã chạy: " + cmd, Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                Toast.makeText(context, "❌ Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }).start();
    }
}
