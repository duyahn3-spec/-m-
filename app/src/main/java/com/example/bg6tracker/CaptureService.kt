package com.example.bg6tracker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.util.Log

class CaptureService : Service() {

    companion object {

        // Action dùng từ MainActivity
        const val ACTION_START =
            "com.example.bg6tracker.START"

        // Intent extras
        const val EXTRA_RESULT_CODE =
            "result_code"

        const val EXTRA_DATA =
            "projection_data"

        // Kích thước capture
        const val WIDTH = 320
        const val HEIGHT = 240

        // Notification
        private const val CHANNEL_ID =
            "bg6_tracker_channel"

        private const val NOTIFICATION_ID =
            1001

        private const val TAG =
            "BG6Tracker"
    }

    // Worker thread riêng.
    // Không xử lý ImageReader trên main thread.
    private lateinit var workerThread: HandlerThread

    private lateinit var workerHandler: Handler

    // MediaProjection
    private var mediaProjection:
        MediaProjection? = null

    // Virtual display
    private var virtualDisplay:
        VirtualDisplay? = null

    // ImageReader
    private var imageReader:
        ImageReader? = null

    // Pipeline
    private var frameProcessor:
        FrameProcessor? = null

    private var detector:
        MoveNetDetector? = null

    // Tracking
    private val kalman =
        KalmanTracker()

    private val pid =
        PIDController()

    // Frame statistics
    private var sequence =
        0

    private var lastFrameNs =
        0L

    private var fpsLastNs =
        0L

    private var fpsFrames =
        0

    private var currentFps =
        0.0

    // =========================================================
    // SERVICE CREATED
    // =========================================================

    override fun onCreate() {
        super.onCreate()

        Log.d(
            TAG,
            "CaptureService created"
        )

        createNotificationChannel()

        startTrackerForeground()

        // Thread chuyên xử lý frame
        workerThread =
            HandlerThread(
                "BG6TrackerWorker"
            )

        workerThread.start()

        workerHandler =
            Handler(
                workerThread.looper
            )

        // Khởi tạo pipeline một lần.
        // Không khởi tạo lại model cho từng frame.
        frameProcessor =
            FrameProcessor()

        detector =
            MoveNetDetector(this)

        kalman.reset()

        pid.reset()
    }

    // =========================================================
    // START COMMAND
    // =========================================================

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int
    ): Int {

        Log.d(
            TAG,
            "onStartCommand"
        )

        if (
            intent?.action ==
            ACTION_START
        ) {

            val resultCode =
                intent.getIntExtra(
                    EXTRA_RESULT_CODE,
                    RESULT_CANCELED
                )

            val projectionData =
                if (
                    Build.VERSION.SDK_INT >=
                    Build.VERSION_CODES.TIRAMISU
                ) {

                    intent.getParcelableExtra(
                        EXTRA_DATA,
                        Intent::class.java
                    )

                } else {

                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(
                        EXTRA_DATA
                    )
                }

            if (
                resultCode ==
                RESULT_OK &&
                projectionData != null
            ) {

                startScreenCapture(
                    resultCode,
                    projectionData
                )

            } else {

                Log.e(
                    TAG,
                    "Invalid MediaProjection permission data"
                )

                stopSelf()
            }
        }

        /*
         * Không tự restart service nếu hệ thống kill.
         * MainActivity sẽ khởi động lại khi người dùng yêu cầu.
         */
        return START_NOT_STICKY
    }

    // =========================================================
    // MEDIA PROJECTION
    // =========================================================

    private fun startScreenCapture(
        resultCode: Int,
        data: Intent
    ) {

        Log.d(
            TAG,
            "Starting MediaProjection"
        )

        // Nếu đang chạy capture cũ thì giải phóng trước.
        stopScreenCapture()

        val projectionManager =
            getSystemService(
                MEDIA_PROJECTION_SERVICE
            ) as MediaProjectionManager

        mediaProjection =
            projectionManager.getMediaProjection(
                resultCode,
                data
            )

        if (mediaProjection == null) {

            Log.e(
                TAG,
                "MediaProjection is null"
            )

            stopSelf()

            return
        }

        /*
         * Theo dõi khi MediaProjection bị thu hồi.
         */
        mediaProjection?.registerCallback(
            object : MediaProjection.Callback() {

                override fun onStop() {

                    Log.d(
                        TAG,
                        "MediaProjection stopped"
                    )

                    workerHandler.post {

                        stopScreenCapture()
                    }
                }
            },
            workerHandler
        )

        createImageReaderAndVirtualDisplay()
    }

    // =========================================================
    // IMAGE READER + VIRTUAL DISPLAY
    // =========================================================

    private fun createImageReaderAndVirtualDisplay() {

        val projection =
            mediaProjection
                ?: return

        /*
         * Chỉ giữ tối đa 2 frame.
         *
         * acquireLatestImage() bên dưới sẽ bỏ frame cũ,
         * giúp giảm tình trạng queue bị trễ.
         */
        imageReader =
            ImageReader.newInstance(
                WIDTH,
                HEIGHT,
                PixelFormat.RGBA_8888,
                2
            )

        imageReader?.setOnImageAvailableListener(
            { reader ->

                /*
                 * acquireLatestImage() rất quan trọng
                 * cho realtime processing.
                 *
                 * Nếu CPU đang xử lý frame trước,
                 * frame cũ không cần thiết sẽ bị bỏ qua.
                 */
                val image =
                    reader.acquireLatestImage()
                        ?: return@setOnImageAvailableListener

                try {

                    processFrame(
                        image
                    )

                } catch (e: Exception) {

                    Log.e(
                        TAG,
                        "Error processing frame",
                        e
                    )

                } finally {

                    /*
                     * LUÔN close Image.
                     *
                     * Nếu không close queue ImageReader
                     * sẽ nhanh chóng bị đầy.
                     */
                    image.close()
                }

            },
            workerHandler
        )

        /*
         * Lấy densityDpi của màn hình.
         *
         * Đây chỉ là thông số VirtualDisplay,
         * kích thước thực tế của capture vẫn là
         * WIDTH × HEIGHT.
         */
        val metrics =
            resources.displayMetrics

        val densityDpi =
            metrics.densityDpi

        /*
         * Tạo VirtualDisplay.
         */
        virtualDisplay =
            projection.createVirtualDisplay(
                "BG6TrackerScreen",

                WIDTH,
                HEIGHT,

                densityDpi,

                DisplayManager
                    .VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,

                imageReader!!.surface,

                null,

                workerHandler
            )

        if (virtualDisplay == null) {

            Log.e(
                TAG,
                "VirtualDisplay creation failed"
            )

            stopScreenCapture()

            return
        }

        Log.d(
            TAG,
            "Capture started: " +
                "${WIDTH}x${HEIGHT}"
        )
    }

    // =========================================================
    // FRAME PROCESSING
    // =========================================================

    private fun processFrame(
        image: Image
    ) {

        val frameStartNs =
            System.nanoTime()

        sequence++

        // -----------------------------------------------------
        // FPS
        // -----------------------------------------------------

        updateFps()

        // -----------------------------------------------------
        // PREPROCESS
        // -----------------------------------------------------

        val preprocessStartNs =
            System.nanoTime()

        val input =
            detector?.createInput()

        if (input == null) {

            Log.e(
                TAG,
                "Detector input buffer is null"
            )

            return
        }

        frameProcessor?.process(
            image,
            input
        )

        val preprocessMs =
            nanosToMs(
                System.nanoTime() -
                    preprocessStartNs
            )

        // -----------------------------------------------------
        // MOVENET
        // -----------------------------------------------------

        val inferenceStartNs =
            System.nanoTime()

        val detection =
            detector?.detect(
                input
            )

        val inferenceMs =
            nanosToMs(
                System.nanoTime() -
                    inferenceStartNs
            )

        if (detection == null) {

            Log.w(
                TAG,
                "No detector result"
            )

            return
        }

        /*
         * MoveNet output:
         *
         * detection[0] = normalized X
         * detection[1] = normalized Y
         * detection[2] = confidence
         */
        val normalizedX =
            detection[0]

        val normalizedY =
            detection[1]

        val confidence =
            detection[2]

        // -----------------------------------------------------
        // FRAME TIME
        // -----------------------------------------------------

        val currentNs =
            System.nanoTime()

        val dtSeconds =
            if (
                lastFrameNs == 0L
            ) {

                1.0 / 30.0

            } else {

                (
                    currentNs -
                        lastFrameNs
                    ) / 1_000_000_000.0
            }

        lastFrameNs =
            currentNs

        // -----------------------------------------------------
        // CONVERT COORDINATES
        // -----------------------------------------------------

        val measuredX =
            (
                normalizedX
                    .coerceIn(
                        0.0f,
                        1.0f
                    ) *
                    WIDTH
                ).toDouble()

        val measuredY =
            (
                normalizedY
                    .coerceIn(
                        0.0f,
                        1.0f
                    ) *
                    HEIGHT
                ).toDouble()

        // -----------------------------------------------------
        // KALMAN
        // -----------------------------------------------------

        val trackingStartNs =
            System.nanoTime()

        val state =
            if (
                confidence >=
                MoveNetDetector
                    .CONFIDENCE_THRESHOLD
            ) {

                kalman.update(
                    measuredX,
                    measuredY,
                    dtSeconds
                )

            } else {

                /*
                 * Nếu confidence thấp,
                 * không cập nhật bằng measurement mới.
                 *
                 * Chỉ dự đoán vị trí.
                 */
                kalman.predict(
                    dtSeconds
                )
            }

        val trackingMs =
            nanosToMs(
                System.nanoTime() -
                    trackingStartNs
            )

        val trackedX =
            state[0]

        val trackedY =
            state[1]

        val velocityX =
            state[2]

        val velocityY =
            state[3]

        // -----------------------------------------------------
        // PID
        // -----------------------------------------------------

        val controlStartNs =
            System.nanoTime()

        val centerX =
            WIDTH / 2.0

        val centerY =
            HEIGHT / 2.0

        val errorX =
            trackedX -
                centerX

        val errorY =
            trackedY -
                centerY

        /*
         * PID chỉ tạo ra vector điều khiển
         * để dùng trong nghiên cứu closed-loop.
         */
        val control =
            pid.update(
                errorX,
                errorY,
                dtSeconds
            )

        val dx =
            control.first

        val dy =
            control.second

        val controlMs =
            nanosToMs(
                System.nanoTime() -
                    controlStartNs
            )

        // -----------------------------------------------------
        // TOTAL
        // -----------------------------------------------------

        val totalMs =
            nanosToMs(
                System.nanoTime() -
                    frameStartNs
            )

        // -----------------------------------------------------
        // LOG
        // -----------------------------------------------------

        Log.d(
            TAG,
            buildString {

                append("frame=")
                append(sequence)

                append(" fps=")
                append(
                    String.format(
                        "%.1f",
                        currentFps
                    )
                )

                append(" x=")
                append(
                    String.format(
                        "%.1f",
                        trackedX
                    )
                )

                append(" y=")
                append(
                    String.format(
                        "%.1f",
                        trackedY
                    )
                )

                append(" vx=")
                append(
                    String.format(
                        "%.1f",
                        velocityX
                    )
                )

                append(" vy=")
                append(
                    String.format(
                        "%.1f",
                        velocityY
                    )
                )

                append(" conf=")
                append(
                    String.format(
                        "%.3f",
                        confidence
                    )
                )

                append(" dx=")
                append(dx)

                append(" dy=")
                append(dy)

                append(" preprocess=")
                append(
                    String.format(
                        "%.2f",
                        preprocessMs
                    )
                )

                append(" ai=")
                append(
                    String.format(
                        "%.2f",
                        inferenceMs
                    )
                )

                append(" tracking=")
                append(
                    String.format(
                        "%.2f",
                        trackingMs
                    )
                )

                append(" control=")
                append(
                    String.format(
                        "%.2f",
                        controlMs
                    )
                )

                append(" total=")
                append(
                    String.format(
                        "%.2f",
                        totalMs
                    )
                )

                append("ms")
            }
        )
    }

    // =========================================================
    // FPS
    // =========================================================

    private fun updateFps() {

        val now =
            System.nanoTime()

        fpsFrames++

        if (fpsLastNs == 0L) {

            fpsLastNs =
                now

            return
        }

        val elapsed =
            now -
                fpsLastNs

        /*
         * Cập nhật FPS mỗi khoảng 1 giây.
         */
        if (
            elapsed >=
            1_000_000_000L
        ) {

            currentFps =
                fpsFrames.toDouble() /
                    (
                        elapsed /
                            1_000_000_000.0
                    )

            fpsFrames =
                0

            fpsLastNs =
                now

            Log.d(
                TAG,
                "FPS=$currentFps"
            )
        }
    }

    // =========================================================
    // NOTIFICATION
    // =========================================================

    private fun createNotificationChannel() {

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            val channel =
                NotificationChannel(
                    CHANNEL_ID,

                    "BG6 Tracker",

                    NotificationManager
                        .IMPORTANCE_LOW
                )

            channel.description =
                "Realtime screen tracking"

            val manager =
                getSystemService(
                    NotificationManager::class.java
                )

            manager.createNotificationChannel(
                channel
            )
        }
    }

    private fun startTrackerForeground() {

        val notification =
            createNotification()

        if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.Q
        ) {

            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo
                    .FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )

        } else {

            startForeground(
                NOTIFICATION_ID,
                notification
            )
        }
    }

    private fun createNotification():
        Notification {

        return if (
            Build.VERSION.SDK_INT >=
            Build.VERSION_CODES.O
        ) {

            Notification.Builder(
                this,
                CHANNEL_ID
            )
                .setContentTitle(
                    "BG6 Realtime Tracker"
                )
  
