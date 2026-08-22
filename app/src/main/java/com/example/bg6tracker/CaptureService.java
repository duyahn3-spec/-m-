package com.example.bg6tracker;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.IBinder;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CaptureService extends Service {

    public static final String EXTRA_RESULT_CODE =
            "result_code";

    public static final String EXTRA_RESULT_DATA =
            "result_data";

    private MediaProjection projection;

    private ImageReader imageReader;

    private android.hardware.display.VirtualDisplay virtualDisplay;

    private ExecutorService executor;

    private FrameProcessor processor;

    private static final int WIDTH = 320;
    private static final int HEIGHT = 240;

    @Override
    public void onCreate() {
        super.onCreate();

        executor =
                Executors.newSingleThreadExecutor();

        try {
            processor =
                    new FrameProcessor(this);
        } catch (Exception e) {
            stopSelf();
        }

        createNotificationChannel();

        startForeground(
                1001,
                createNotification()
        );
    }

    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId) {

        int resultCode =
                intent.getIntExtra(
                        EXTRA_RESULT_CODE,
                        -1
                );

        Intent resultData =
                intent.getParcelableExtra(
                        EXTRA_RESULT_DATA
                );

        if (resultCode == -1 ||
                resultData == null) {

            stopSelf();
            return START_NOT_STICKY;
        }

        startCapture(
                resultCode,
                resultData
        );

        return START_STICKY;
    }

    private void startCapture(
            int resultCode,
            Intent data) {

        MediaProjectionManager manager =
                (MediaProjectionManager)
                        getSystemService(
                                MEDIA_PROJECTION_SERVICE
                        );

        projection =
                manager.getMediaProjection(
                        resultCode,
                        data
                );

        imageReader =
                ImageReader.newInstance(
                        WIDTH,
                        HEIGHT,
                        PixelFormat.RGBA_8888,
                        2
                );

        imageReader.setOnImageAvailableListener(
                reader -> {

                    Image image =
                            reader.acquireLatestImage();

                    if (image == null) {
                        return;
                    }

                    executor.execute(() -> {

                        try {
                            Bitmap bitmap =
                                    imageToBitmap(image);

                            if (bitmap != null &&
                                    processor != null) {

                                processor.process(bitmap);

                                bitmap.recycle();
                            }

                        } finally {
                            image.close();
                        }
                    });

                },
                null
        );

        Surface surface =
                imageReader.getSurface();

        virtualDisplay =
                projection.createVirtualDisplay(
                        "BG6Tracker",
                        WIDTH,
                        HEIGHT,
                        getResources()
                                .getDisplayMetrics()
                                .densityDpi,
                        0,
                        surface,
                        null,
                        null
                );
    }

    private Bitmap imageToBitmap(
            Image image) {

        Image.Plane plane =
                image.getPlanes()[0];

        ByteBuffer buffer =
                plane.getBuffer();

        int pixelStride =
                plane.getPixelStride();

        int rowStride =
                plane.getRowStride();

        int rowPadding =
                rowStride -
                pixelStride * WIDTH;

        Bitmap bitmap =
                Bitmap.createBitmap(
                        WIDTH +
                                rowPadding /
                                        pixelStride,
                        HEIGHT,
                        Bitmap.Config.ARGB_8888
                );

        bitmap.copyPixelsFromBuffer(buffer);

        return Bitmap.createBitmap(
                bitmap,
                0,
                0,
                WIDTH,
                HEIGHT
        );
    }

    private Notification createNotification() {

        if (Build.VERSION.SDK_INT >= 26) {

            NotificationChannel channel =
                    new NotificationChannel(
                            "capture",
                            "Screen Capture",
                            NotificationManager
                                    .IMPORTANCE_LOW
                    );

            NotificationManager manager =
                    getSystemService(
                            NotificationManager.class
                    );

            manager.createNotificationChannel(
                    channel
            );
        }

        return new Notification.Builder(
                this,
                "capture"
        )
                .setContentTitle("BG6 Tracker")
                .setContentText(
                        "Đang xử lý luồng màn hình"
                )
                .setSmallIcon(
                        android.R.drawable.ic_menu_view
                )
                .build();
    }

    private void createNotificationChannel() {
        // Channel được tạo trong createNotification().
    }

    @Override
    public void onDestroy() {

        if (virtualDisplay != null) {
            virtualDisplay.release();
        }

        if (imageReader != null) {
            imageReader.close();
        }

        if (projection != null) {
            projection.stop();
        }

        if (executor != null) {
            executor.shutdownNow();
        }

        if (processor != null) {
            processor.close();
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
          }
