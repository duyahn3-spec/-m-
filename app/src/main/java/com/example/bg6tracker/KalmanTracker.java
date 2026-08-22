package com.example.bg6tracker;

public class KalmanTracker {

    private float x;
    private float y;
    private float vx;
    private float vy;

    private boolean initialized = false;

    private final float q = 0.4f;
    private final float r = 1.5f;

    public synchronized void update(
            float measurementX,
            float measurementY,
            float dt) {

        if (!initialized) {
            x = measurementX;
            y = measurementY;
            vx = 0;
            vy = 0;
            initialized = true;
            return;
        }

        if (dt <= 0 || dt > 0.2f) {
            dt = 0.01f;
        }

        float predictedX = x + vx * dt;
        float predictedY = y + vy * dt;

        float errorX = measurementX - predictedX;
        float errorY = measurementY - predictedY;

        float gain = q / (q + r);

        x = predictedX + gain * errorX;
        y = predictedY + gain * errorY;

        vx = vx + gain * errorX / dt;
        vy = vy + gain * errorY / dt;
    }

    public synchronized float predict(float seconds) {
        return x + vx * seconds;
    }

    public synchronized float getX() {
        return x;
    }

    public synchronized float getY() {
        return y;
    }

    public synchronized float getVelocityX() {
        return vx;
    }

    public synchronized float getVelocityY() {
        return vy;
    }

    public synchronized boolean isInitialized() {
        return initialized;
    }
}
