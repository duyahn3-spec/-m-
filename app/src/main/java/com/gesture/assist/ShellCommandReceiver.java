package com.gesture.assist;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ShellCommandReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        showShellDialog(context);
    }

    private void showShellDialog(Context context) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("📟 Nhập lệnh shell (qua Shizuku / Runtime)");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(30, 20, 30, 20);

        final EditText input = new EditText(context);
        input.setHint("vd: settings put system pointer_speed 20");
        input.setTextSize(16f);

        final TextView outputView = new TextView(context);
        outputView.setText("▶ Kết quả sẽ hiện ở đây");
        outputView.setTextSize(14f);
        outputView.setTextColor(0xFF00FF00);
        outputView.setPadding(10, 20, 10, 10);

        layout.addView(input);
        layout.addView(outputView);

        builder.setView(layout);

        builder.setPositiveButton("▶ Chạy", (dialog, which) -> {
            String cmd = input.getText().toString().trim();
            if (!cmd.isEmpty()) {
                executeShell(cmd, context, outputView);
            } else {
                Toast.makeText(context, "⚠️ Nhập lệnh trước khi chạy!", Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("❌ Đóng", null);
        builder.show();
    }

    private void executeShell(String cmd, Context context, TextView outputView) {
        Toast.makeText(context, "⏳ Đang chạy: " + cmd, Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            try {
                Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
                process.waitFor();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                StringBuilder output = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    output.append("❌ ").append(errorLine).append("\n");
                }

                final String result = output.length() > 0 ? output.toString() : "✅ Lệnh đã chạy thành công (không có output)";
                new Handler(Looper.getMainLooper()).post(() -> {
                    outputView.setText("▶ " + result);
                    Toast.makeText(context, "✅ Đã chạy xong!", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    outputView.setText("❌ Lỗi: " + e.getMessage());
                    Toast.makeText(context, "❌ Lỗi: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }
}
