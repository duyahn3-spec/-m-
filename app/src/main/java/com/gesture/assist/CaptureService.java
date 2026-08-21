package com.gesture.assist;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.tensorflow.lite.Interpreter; // TensorFlow Lite
import java.nio.MappedByteBuffer;
import java.io.FileInputStream;
import java.nio.channels.FileChannel;

public class CaptureService extends Service {

    private static final String TAG = "CaptureService";
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Interpreter tflite;
    private BLEController bleController;
    private Handler handler = new Handler(Looper.getMainLooper());
    private ExecutorService executor = Executors.newSingleThreadExecutor();

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(1, new NotificationCompat.Builder(this, "channel_id")
                .setContentTitle("Aim Assist")
                .setContentText("Running...")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .build());

        // Load TensorFlow Lite model
        try {
            MappedByteBuffer model = loadModelFile("movenet_lightning_int8.tflite");
            tflite = new Interpreter(model);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load model", e);
            stopSelf();
        }

        // Khởi tạo BLE Controller
        bleController = new BLEController();
        bleController.init();
    }

    private MappedByteBuffer loadModelFile(String modelPath) throws Exception {
        FileInputStream fis = new FileInputStream(getAssets().openFd(modelPath).getFileDescriptor());
        FileChannel fc = fis.getChannel();
        return fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int resultCode = intent.getIntExtra("resultCode", 0);
        Intent data = intent.getParcelableExtra("data");
        startScreenCapture(resultCode, data);
        return START_STICKY;
    }

    private void startScreenCapture(int resultCode, Intent data) {
        MediaProjectionManager projectionManager = (MediaProjectionManager) getSystemService(MEDIA_PROJECTION_SERVICE);
        mediaProjection = projectionManager.getMediaProjection(resultCode, data);

        int width = 320;
        int height = 240;
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        imageReader.setOnImageAvailableListener(reader -> {
            Image image = reader.acquireLatestImage();
            if (image != null) {
                executor.execute(() -> processImage(image));
                image.close();
            }
        }, handler);

        virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                width, height, 320,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null, null
        );
    }

    private void processImage(Image image) {
        long startTime = System.currentTimeMillis();

        // Chuyển Image -> Bitmap
        Image.Plane[] planes = image.getPlanes();
        ByteBuffer buffer = planes[0].getBuffer();
        int pixelStride = planes[0].getPixelStride();
        int rowStride = planes[0].getRowStride();
        int rowPadding = rowStride - pixelStride * 320;

        Bitmap bitmap = Bitmap.createBitmap(320 + rowPadding / pixelStride, 240, Bitmap.Config.ARGB_8888);
        bitmap.copyPixelsFromBuffer(buffer);

        // Resize về 160x120 cho AI
        Bitmap resized = Bitmap.createScaledBitmap(bitmap, 160, 120, true);
        int[] pixels = new int[160 * 120];
        resized.getPixels(pixels, 0, 160, 0, 0, 160, 120);

        // Chạy MoveNet INT8 (giả sử input shape [1, 192, 192, 3])
        float[][][][] input = new float[1][192][192][3];
        for (int y = 0; y < 120; y++) {
            for (int x = 0; x < 160; x++) {
                int p = pixels[y * 160 + x];
                int r = (p >> 16) & 0xFF;
                int g = (p >> 8) & 0xFF;
                int b = p & 0xFF;
                input[0][y][x][0] = r / 255.0f;
                input[0][y][x][1] = g / 255.0f;
                input[0][y][x][2] = b / 255.0f;
            }
        }

        float[][][][] output = new float[1][17][3]; // MoveNet output [1, 17, 3]
        tflite.run(input, output);

        // Lấy tọa độ mũi (index 0)
        float conf = output[0][0][2];
        if (conf > 0.3f) {
            float noseX = output[0][0][1] * 160;
            float noseY = output[0][0][0] * 120;

            // Tính dx, dy (lệch so với tâm màn hình 160x120)
            float dx = noseX - 80;
            float dy = noseY - 60;

            // Gửi qua BLE đến ESP32
            bleController.sendDelta((int)dx, (int)dy);

            long endTime = System.currentTimeMillis();
            Log.d(TAG, "Latency: " + (endTime - startTime) + "ms, dx=" + dx + ", dy=" + dy);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (virtualDisplay != null) virtualDisplay.release();
        if (mediaProjection != null) mediaProjection.stop();
        if (imageReader != null) imageReader.close();
        if (tflite != null) tflite.close();
        if (bleController != null) bleController.close();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
                  }
