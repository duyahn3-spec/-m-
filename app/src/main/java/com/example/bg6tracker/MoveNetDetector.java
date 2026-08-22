package com.example.bg6tracker;

import android.content.Context;
import android.graphics.Bitmap;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class MoveNetDetector {

    private static final int INPUT_SIZE = 192;

    private final Interpreter interpreter;

    private final byte[][][][] input =
            new byte[1][INPUT_SIZE][INPUT_SIZE][3];

    private final float[][][] output =
            new float[1][17][3];

    public MoveNetDetector(Context context)
            throws IOException {

        MappedByteBuffer model =
                loadModel(context);

        Interpreter.Options options =
                new Interpreter.Options();

        options.setNumThreads(2);

        interpreter =
                new Interpreter(model, options);
    }

    private MappedByteBuffer loadModel(Context context)
            throws IOException {

        FileInputStream inputStream =
                (FileInputStream) context
                        .getAssets()
                        .openFd("movenet_lightning_int8.tflite")
                        .createInputStream();

        FileChannel channel =
                inputStream.getChannel();

        long start =
                context.getAssets()
                        .openFd("movenet_lightning_int8.tflite")
                        .getStartOffset();

        long length =
                context.getAssets()
                        .openFd("movenet_lightning_int8.tflite")
                        .getLength();

        return channel.map(
                FileChannel.MapMode.READ_ONLY,
                start,
                length
        );
    }

    public synchronized TrackingResult detect(
            Bitmap bitmap) {

        Bitmap resized =
                Bitmap.createScaledBitmap(
                        bitmap,
                        INPUT_SIZE,
                        INPUT_SIZE,
                        true
                );

        for (int y = 0; y < INPUT_SIZE; y++) {

            for (int x = 0; x < INPUT_SIZE; x++) {

                int pixel =
                        resized.getPixel(x, y);

                input[0][y][x][0] =
                        (byte) ((pixel >> 16) & 0xFF);

                input[0][y][x][1] =
                        (byte) ((pixel >> 8) & 0xFF);

                input[0][y][x][2] =
                        (byte) (pixel & 0xFF);
            }
        }

        interpreter.run(input, output);

        float y = output[0][0][0];
        float x = output[0][0][1];
        float confidence = output[0][0][2];

        return new TrackingResult(
                x,
                y,
                confidence,
                confidence >= 0.30f
        );
    }

    public void close() {
        interpreter.close();
    }
}
