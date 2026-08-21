package com.example.bg6tracker

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {

    companion object {
        private const val REQUEST_MEDIA_PROJECTION = 1001
        private const val REQUEST_BLUETOOTH = 1002
    }

    private lateinit var projectionManager:
        MediaProjectionManager

    private lateinit var statusText:
        TextView

    private lateinit var metricsText:
        TextView

    private lateinit var startButton:
        Button

    private lateinit var stopButton:
        Button

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_main
        )

        statusText =
            findViewById(R.id.statusText)

        metricsText =
            findViewById(R.id.metricsText)

        startButton =
            findViewById(R.id.startButton)

        stopButton =
            findViewById(R.id.stopButton)

        projectionManager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        requestBluetoothPermission()

        startButton.setOnClickListener {
            requestScreenCapture()
        }

        stopButton.setOnClickListener {
            stopCapture()
        }

        statusText.text =
            "Status: Ready"

        metricsText.text =
            """
            BG6 Realtime Tracker

            Capture: 320 × 240
            Model: MoveNet Lightning INT8
            Tracking: Kalman
            Controller: PID
            BLE: Ready

            Chưa bắt đầu capture.
            """.trimIndent()
    }

    private fun requestBluetoothPermission() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.S
        ) {

            val permissions = arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )

            val missing =
                permissions.filter {
                    checkSelfPermission(it) !=
                        PackageManager.PERMISSION_GRANTED
                }

            if (missing.isNotEmpty()) {

                requestPermissions(
                    missing.toTypedArray(),
                    REQUEST_BLUETOOTH
                )
            }
        }
    }

    private fun requestScreenCapture() {

        statusText.text =
            "Status: Waiting for screen permission..."

        val intent =
            projectionManager
                .createScreenCaptureIntent()

        startActivityForResult(
            intent,
            REQUEST_MEDIA_PROJECTION
        )
    }

    @Deprecated(
        "Deprecated in Android API",
        ReplaceWith("")
    )
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {

        super.onActivityResult(
            requestCode,
            resultCode,
            data
        )

        if (
            requestCode !=
            REQUEST_MEDIA_PROJECTION
        ) {
            return
        }

        if (
            resultCode != RESULT_OK ||
            data == null
        ) {

            statusText.text =
                "Status: Screen permission denied"

            return
        }

        startCaptureService(
            resultCode,
            data
        )
    }

    private fun startCaptureService(
        resultCode: Int,
        data: Intent
    ) {

        val serviceIntent =
            Intent(
                this,
                CaptureService::class.java
            )

        serviceIntent.action =
            CaptureService.ACTION_START

        serviceIntent.putExtra(
            CaptureService.EXTRA_RESULT_CODE,
            resultCode
        )

        serviceIntent.putExtra(
            CaptureService.EXTRA_DATA,
            data
        )

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            startForegroundService(
                serviceIntent
            )

        } else {

            startService(
                serviceIntent
            )
        }

        statusText.text =
            "Status: Screen capture running"

        metricsText.text =
            """
            Capture started.

            Resolution: 320 × 240
            Target FPS: 30

            Đang khởi tạo:
            • ImageReader
            • MoveNet INT8
            • Kalman
            • PID
            """.trimIndent()

        startButton.isEnabled =
            false

        stopButton.isEnabled =
            true
    }

    private fun stopCapture() {

        val intent =
            Intent(
                this,
                CaptureService::class.java
            )

        stopService(intent)

        statusText.text =
            "Status: Stopped"

        metricsText.text =
            """
            Capture stopped.

            Nhấn Start để chạy lại.
            """.trimIndent()

        startButton.isEnabled =
            true

        stopButton.isEnabled =
            false
    }

    override fun onDestroy() {

        /*
         * Không tự stop service ở đây.
         *
         * Người dùng có thể rời Activity
         * nhưng foreground service vẫn tiếp tục.
         */

        super.onDestroy()
    }
}
