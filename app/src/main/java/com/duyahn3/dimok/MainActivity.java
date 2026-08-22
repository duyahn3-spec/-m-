package com.duyahn3.dimok;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final int REQUEST_MEDIA_PROJECTION = 1001;

    private MediaProjectionManager projectionManager;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        projectionManager =
                (MediaProjectionManager) getSystemService(
                        Context.MEDIA_PROJECTION_SERVICE
                );

        createInterface();
    }

    private void createInterface() {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        statusText = new TextView(this);
        statusText.setText(
                "Screen Tracker\n\n" +
                "Trạng thái: Chưa bắt đầu"
        );
        statusText.setTextSize(18);

        Button startButton = new Button(this);
        startButton.setText("Bắt đầu chia sẻ màn hình");

        Button stopButton = new Button(this);
        stopButton.setText("Dừng");

        layout.addView(statusText);
        layout.addView(startButton);
        layout.addView(stopButton);

        setContentView(layout);

        startButton.setOnClickListener(v -> requestScreenCapture());

        stopButton.setOnClickListener(v -> {
            statusText.setText(
                    "Screen Tracker\n\n" +
                    "Trạng thái: Đã dừng"
            );
        });
    }

    private void requestScreenCapture() {

        if (projectionManager == null) {
            statusText.setText(
                    "Không thể khởi tạo MediaProjection."
            );
            return;
        }

        Intent captureIntent =
                projectionManager.createScreenCaptureIntent();

        startActivityForResult(
                captureIntent,
                REQUEST_MEDIA_PROJECTION
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {
        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode != REQUEST_MEDIA_PROJECTION) {
            return;
        }

        if (resultCode == RESULT_OK && data != null) {

            statusText.setText(
                    "Screen Tracker\n\n" +
                    "Trạng thái: Đã cấp quyền màn hình\n\n" +
                    "MediaProjection đã sẵn sàng."
            );

            Intent serviceIntent =
                    new Intent(
                            this,
                            CaptureService.class
                    );

            serviceIntent.putExtra(
                    "resultCode",
                    resultCode
            );

            serviceIntent.putExtra(
                    "data",
                    data
            );

            if (android.os.Build.VERSION.SDK_INT >= 26) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }

        } else {

            statusText.setText(
                    "Screen Tracker\n\n" +
                    "Trạng thái: Người dùng từ chối quyền màn hình"
            );
        }
    }
        }
