package com.example.bg6tracker;

import android.content.Context;
import android.graphics.Bitmap;

public class FrameProcessor {

    private final MoveNetDetector detector;
    private final KalmanTracker tracker;
    private final PIDController pid;

    private long previousTime;

    public FrameProcessor(Context context)
            throws Exception {

        detector =
                new MoveNetDetector(context);

        tracker =
                new KalmanTracker();

        pid =
                new PIDController(
                        0.75f,
                        0.001f,
                        0.28f,
                        250f,
                        3f
                );

        previousTime =
                System.nanoTime();
    }

    public synchronized TrackingResult process(
            Bitmap bitmap) {

        long now =
                System.nanoTime();

        float dt =
                (now - previousTime)
                        / 1_000_000_000.0f;

        previousTime = now;

        TrackingResult result =
                detector.detect(bitmap);

        if (!result.valid) {
            return result;
        }

        tracker.update(
                result.x,
                result.y,
                dt
        );

        return new TrackingResult(
                tracker.getX(),
                tracker.getY(),
                result.confidence,
                true
        );
    }

    public void close() {
        detector.close();
    }
}
