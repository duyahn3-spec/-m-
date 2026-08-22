package com.example.bg6tracker;

import android.app.Activity;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final int REQUEST_CAPTURE = 1001;

    private MediaProjectionManager projectionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        projectionManager =
                (MediaProjectionManager)
                        getSystemService(
                                MEDIA_PROJECTION_SERVICE
                        );

        LinearLayout layout =
                new LinearLayout(this);

        layout.setOrientation(
                LinearLayout.VERTICAL
        );

        layout.setPadding(
                40,
                60,
                40,
                40
        );

        TextView title =
                new TextView(this);

        title.setText(
                "BG6 Screen Tracker"
        );

        title.setTextSize(24);

        Button start =
                new Button(this);

        start.setText(
                "Bắt đầu thu màn hình"
        );

        start.setOnClickListener(
                v -> requestCapture()
        );

        layout.addView(title);
        layout.addView(start);

        setContentView(layout);
    }

    private void requestCapture() {

        Intent intent =
                projectionManager
                        .createScreenCaptureIntent();

        startActivityForResult(
                intent,
                REQUEST_CAPTURE
        );
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode != REQUEST_CAPTURE) {
            return;
        }

        if (resultCode != RESULT_OK ||
                data == null) {
            return;
        }

        Intent service =
                new Intent(
                        this,
                        CaptureService.class
                );

        service.putExtra(
                CaptureService.EXTRA_RESULT_CODE,
                resultCode
        );

        service.putExtra(
                CaptureService.EXTRA_RESULT_DATA,
                data
        );

        if (android.os.Build.VERSION.SDK_INT >= 26) {
            startForegroundService(service);
        } else {
            startService(service);
        }
    }
}
