package com.example.bg6tracker

import android.app.Activity
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView

class MainActivity : Activity() {

    companion object {

        private const val REQUEST_CAPTURE = 500
    }

    private lateinit var projectionManager:
        MediaProjectionManager

    private lateinit var status:
        TextView

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(
            savedInstanceState
        )

        setContentView(
            R.layout.activity_main
        )

        status =
            findViewById(
                R.id.statusText
            )

        val start =
            findViewById<Button>(
                R.id.startButton
            )

        val stop =
            findViewById<Button>(
                R.id.stopButton
            )

        projectionManager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        start.setOnClickListener {

            val intent =
                projectionManager
                    .createScreenCaptureIntent()

            startActivityForResult(
                intent,
                REQUEST_CAPTURE
            )
        }

        stop.setOnClickListener {

            stopService(
                Intent(
                    this,
                    CaptureService::class.java
                )
            )

            status.text =
                "Status: Stopped"

            start.isEnabled = true
            stop.isEnabled = false
        }
    }

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
            requestCode ==
            REQUEST_CAPTURE &&
            resultCode ==
            RESULT_OK &&
            data != null
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

            startForegroundService(
                serviceIntent
            )

            status.text =
                "Status: Capturing"

            findViewById<Button>(
                R.id.startButton
            ).isEnabled = false

            findViewById<Button>(
                R.id.stopButton
            ).isEnabled = true
        }
    }
}
