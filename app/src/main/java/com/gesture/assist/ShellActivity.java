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
    }

    private void executeCommand() {
        final String cmd = inputCommand.getText().toString().trim();
        if (cmd.isEmpty()) {
            Toast.makeText(this, "⚠️ Nhập lệnh!", Toast.LENGTH_SHORT).show();
            return;
        }

        outputView.append("\n$ " + cmd + "\n");
        inputCommand.setText("");
        runButton.setEnabled(false);

        new Thread(() -> {
            String result = ShizukuShell.runCommand(cmd);
            final String finalResult = result.isEmpty() ? "✅ Done" : result;
            new Handler(Looper.getMainLooper()).post(() -> {
                outputView.append(finalResult + "\n");
                scrollView.fullScroll(View.FOCUS_DOWN);
                runButton.setEnabled(true);
                Toast.makeText(ShellActivity.this, "✅ Xong!", Toast.LENGTH_SHORT).show();
            });
        }).start();
    }
}
