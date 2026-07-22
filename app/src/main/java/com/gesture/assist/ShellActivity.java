package com.gesture.assist;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ShellActivity extends Activity {
    private EditText inputCommand;
    private TextView outputView;
    private Button runButton, clearButton;
    private ScrollView scrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shell);

        inputCommand = findViewById(R.id.inputCommand);
        outputView = findViewById(R.id.outputView);
        runButton = findViewById(R.id.runButton);
        clearButton = findViewById(R.id.clearButton);
        scrollView = findViewById(R.id.scrollView);

        runButton.setOnClickListener(v -> executeCommand());
        clearButton.setOnClickListener(v -> {
            outputView.setText("");
            inputCommand.setText("");
        });

        String cmd = getIntent().getStringExtra("command");
        if (cmd != null && !cmd.isEmpty()) {
            inputCommand.setText(cmd);
        }
    }

    private void executeCommand() {
        final String cmd = inputCommand.getText().toString().trim();
        if (cmd.isEmpty()) {
            Toast.makeText(this, "⚠️ Nhập lệnh trước khi chạy!", Toast.LENGTH_SHORT).show();
            return;
        }

        outputView.append("\n$ " + cmd + "\n");
        inputCommand.setText("");
        runButton.setEnabled(false);

        new Thread(() -> {
            try {
                Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd});
                process.waitFor();

                StringBuilder output = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String errorLine;
                while ((errorLine = errorReader.readLine()) != null) {
                    output.append("❌ ").append(errorLine).append("\n");
                }

                final String result = output.length() > 0 ? output.toString() : "✅ Done (no output)";
                new Handler(Looper.getMainLooper()).post(() -> {
                    outputView.append(result + "\n");
                    scrollView.fullScroll(View.FOCUS_DOWN);
                    runButton.setEnabled(true);
                    Toast.makeText(ShellActivity.this, "✅ Đã chạy xong!", Toast.LENGTH_SHORT).show();
                });

            } catch (Exception e) {
                new Handler(Looper.getMainLooper()).post(() -> {
                    outputView.append("❌ Lỗi: " + e.getMessage() + "\n");
                    scrollView.fullScroll(View.FOCUS_DOWN);
                    runButton.setEnabled(true);
                });
            }
        }).start();
    }
}
